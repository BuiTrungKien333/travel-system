import { z } from "zod";
import { ApiError } from "../lib/errors";

export function serverConfig() {
  const backend = new URL(process.env.BACKEND_URL ?? "http://127.0.0.1:18081");
  const origin = new URL(process.env.ADMIN_ORIGIN ?? "http://localhost:3001");
  for (const url of [backend, origin])
    if (
      !["http:", "https:"].includes(url.protocol) ||
      url.username ||
      url.password ||
      url.search ||
      url.hash ||
      url.pathname !== "/"
    )
      throw new ApiError("UPSTREAM", 503);
  if (process.env.NODE_ENV === "production") {
    if (!process.env.BACKEND_URL || !process.env.ADMIN_ORIGIN)
      throw new ApiError("UPSTREAM", 503);
    for (const url of [backend, origin])
      if (
        url.protocol !== "https:" &&
        !["localhost", "127.0.0.1", "[::1]"].includes(url.hostname)
      )
        throw new ApiError("UPSTREAM", 503);
  }
  return {
    backend: backend.origin,
    origin: origin.origin,
    secure: origin.protocol === "https:",
  };
}
export async function backendRequest(
  path: string,
  options: {
    method?: "GET" | "POST" | "PATCH";
    token?: string;
    body?: unknown;
  } = {},
) {
  if (
    !/^\/api\/v1\/(auth\/(login|refresh|logout)|admin\/(session|overview|ai\/executions|knowledge|catalog(?:\/[a-f0-9-]{36})?|planning\/retrieval\/(index|search)|users(?:\/[a-f0-9-]{36})?))(?:\?.*)?$/i.test(
      path,
    )
  )
    throw new ApiError("AUTH_002", 403);
  let response: Response;
  try {
    response = await fetch(serverConfig().backend + path, {
      method: options.method ?? "GET",
      headers: {
        Accept: "application/json",
        "Accept-Language": "vi",
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...(options.token ? { Authorization: `Bearer ${options.token}` } : {}),
      },
      body: options.body ? JSON.stringify(options.body) : undefined,
      cache: "no-store",
      redirect: "error",
      signal: AbortSignal.timeout(path.endsWith("/index") ? 180000 : 12000),
    });
  } catch {
    throw new ApiError("UPSTREAM", 503);
  }
  if (!response.ok) {
    const result = z
      .object({ code: z.string().max(64) })
      .safeParse(await response.json().catch(() => null));
    throw new ApiError(
      result.success ? result.data.code : "UPSTREAM",
      response.status,
    );
  }
  if (response.status === 204) return null;
  try {
    return (await response.json()) as unknown;
  } catch {
    throw new ApiError("INVALID_RESPONSE", 502);
  }
}
