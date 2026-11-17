import { z } from "zod";
import { ApiError } from "./errors";
let refreshing: Promise<void> | undefined;
let generation = 0;
let closed = false;
export function invalidateClientSession() {
  generation++;
  closed = true;
}
export function openClientSession() {
  generation++;
  closed = false;
}
export function announceSession(type: "login" | "logout") {
  if (typeof BroadcastChannel !== "undefined") {
    const channel = new BroadcastChannel("wego-admin-session");
    channel.postMessage(type);
    channel.close();
  }
}
async function sessionLock<T>(action: () => Promise<T>) {
  if (typeof navigator !== "undefined" && navigator.locks)
    return navigator.locks.request("wego-admin-session", action);
  return action();
}
async function raw(
  path: string,
  options: { method?: string; body?: unknown; signal?: AbortSignal } = {},
) {
  let response: Response;
  try {
    response = await fetch(`/admin/api${path}`, {
      method: options.method ?? "GET",
      credentials: "same-origin",
      cache: "no-store",
      headers:
        options.body !== undefined
          ? { "Content-Type": "application/json" }
          : undefined,
      body:
        options.body !== undefined ? JSON.stringify(options.body) : undefined,
      signal:
        options.signal ??
        AbortSignal.timeout(path.endsWith("/index") ? 190000 : 15000),
    });
  } catch (error) {
    if (options.signal?.aborted) throw error;
    throw new ApiError("UPSTREAM", 503);
  }
  const body: unknown = await response.json().catch(() => null);
  if (!response.ok) {
    const error = z.object({ code: z.string() }).safeParse(body);
    throw new ApiError(
      error.success ? error.data.code : "UPSTREAM",
      response.status,
    );
  }
  return body;
}
async function refreshSession() {
  if (!refreshing) {
    const version = generation;
    refreshing = sessionLock(async () => {
      if (closed || version !== generation) throw new ApiError("AUTH_001", 401);
      await raw("/auth/refresh", { method: "POST" });
      if (closed || version !== generation) throw new ApiError("AUTH_001", 401);
    }).finally(() => {
      refreshing = undefined;
    });
  }
  return refreshing;
}
export async function apiRequest(
  path: string,
  options: { method?: string; body?: unknown; signal?: AbortSignal } = {},
) {
  const version = generation;
  try {
    const result = await raw(path, options);
    if (version !== generation) throw new ApiError("AUTH_001", 401);
    return result;
  } catch (error) {
    if (
      !(error instanceof ApiError) ||
      error.status !== 401 ||
      closed ||
      version !== generation
    )
      throw error;
    await refreshSession();
    if (version !== generation) throw new ApiError("AUTH_001", 401);
    return raw(path, options);
  }
}
export async function signIn(body: unknown) {
  return sessionLock(async () => {
    const result = await raw("/auth/login", { method: "POST", body });
    openClientSession();
    return result;
  });
}
export async function signOut() {
  invalidateClientSession();
  return sessionLock(async () => {
    const result = await raw("/auth/logout", { method: "POST" });
    announceSession("logout");
    return z
      .object({
        success: z.literal(true),
        data: z.object({ revoked: z.boolean() }),
      })
      .parse(result).data;
  });
}
