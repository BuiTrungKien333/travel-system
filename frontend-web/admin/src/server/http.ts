import { NextRequest, NextResponse } from "next/server";
import { ApiError, errorMessage } from "../lib/errors";
import { serverConfig } from "./backend";

export const cookieNames = {
  access: "wego_admin_access",
  refresh: "wego_admin_refresh",
};

export function json(value: unknown, status = 200) {
  return NextResponse.json(value, {
    status,
    headers: { "Cache-Control": "no-store, private", Pragma: "no-cache" },
  });
}

export function failure(error: unknown) {
  const safe =
    error instanceof ApiError ? error : new ApiError("UPSTREAM", 503);

  return json(
    { success: false, code: safe.code, message: errorMessage(safe) },
    safe.status,
  );
}

export function assertSameOrigin(request: NextRequest) {
  if (
    request.headers.get("origin") !== serverConfig().origin ||
    request.headers.get("sec-fetch-site") === "cross-site"
  )
    throw new ApiError("CSRF", 403);
}

export async function readBody(request: NextRequest) {
  if (
    !request.headers
      .get("content-type")
      ?.toLowerCase()
      .startsWith("application/json")
  )
    throw new ApiError("COMMON_003", 415);
  if (!request.body) throw new ApiError("COMMON_003", 400);
  const reader = request.body.getReader();
  const chunks: Uint8Array[] = [];
  let bytes = 0;
  try {
    while (true) {
      const part = await reader.read();
      if (part.done) break;
      bytes += part.value.byteLength;
      if (bytes > 16384) {
        await reader.cancel();
        throw new ApiError("COMMON_003", 413);
      }
      chunks.push(part.value);
    }
    const body = new Uint8Array(bytes);
    let offset = 0;
    for (const part of chunks) {
      body.set(part, offset);
      offset += part.length;
    }

    return JSON.parse(new TextDecoder().decode(body)) as unknown;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    throw new ApiError("COMMON_003", 400);
  } finally {
    reader.releaseLock();
  }
}

export function clearSession(response: NextResponse) {
  for (const name of Object.values(cookieNames))
    response.cookies.set(name, "", {
      httpOnly: true,
      secure: serverConfig().secure,
      sameSite: "strict",
      path: "/admin",
      maxAge: 0,
    });

  return response;
}

export function setSession(
  response: NextResponse,
  auth: { accessToken: string; refreshToken: string; expiresIn: number },
) {
  const options = {
    httpOnly: true,
    secure: serverConfig().secure,
    sameSite: "strict" as const,
    path: "/admin",
  };

  response.cookies.set(cookieNames.access, auth.accessToken, {
    ...options,
    maxAge: Math.max(1, Math.floor(auth.expiresIn / 1000)),
  });

  // The upstream refresh JWT remains authoritative; the cookie cannot extend its validity.
  response.cookies.set(cookieNames.refresh, auth.refreshToken, {
    ...options,
    maxAge: 7 * 86400,
  });

  return response;
}

export function accessToken(request: NextRequest) {
  const token = request.cookies.get(cookieNames.access)?.value;
  if (!token || token.length > 8192) throw new ApiError("AUTH_001", 401);
  return token;
}
