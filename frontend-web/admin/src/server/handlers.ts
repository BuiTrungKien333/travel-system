import { NextRequest } from "next/server";
import {
  authSchema,
  changeAccountSchema,
  envelope,
  idSchema,
  loginSchema,
  sessionSchema,
  accountPageSchema,
} from "../lib/contracts";
import { ApiError } from "../lib/errors";
import { backendRequest } from "./backend";
import {
  accessToken,
  assertSameOrigin,
  clearSession,
  cookieNames,
  failure,
  json,
  readBody,
  setSession,
} from "./http";

async function current(token: string) {
  const result = envelope(sessionSchema).safeParse(
    await backendRequest("/api/v1/admin/session", { token }),
  );
  if (!result.success) throw new ApiError("INVALID_RESPONSE", 502);

  return result.data.data;
}

async function revoke(refreshToken: string) {
  await backendRequest("/api/v1/auth/logout", {
    method: "POST",
    body: { refreshToken },
  });
}

export async function login(request: NextRequest) {
  try {
    assertSameOrigin(request);
    const credentials = loginSchema.safeParse(await readBody(request));
    if (!credentials.success) throw new ApiError("COMMON_003", 400);
    const result = envelope(authSchema).safeParse(
      await backendRequest("/api/v1/auth/login", {
        method: "POST",
        body: credentials.data,
      }),
    );
    if (!result.success) throw new ApiError("INVALID_RESPONSE", 502);
    const auth = result.data.data;
    try {
      if (auth.user.role !== "ADMIN") throw new ApiError("AUTH_002", 403);
      const user = await current(auth.accessToken);
      return setSession(json({ success: true, data: user }), auth);
    } catch (error) {
      try {
        await revoke(auth.refreshToken);
      } catch {
        console.warn(
          "Admin sign-in cleanup could not revoke the new upstream session.",
        );
      }
      throw error;
    }
  } catch (error) {
    return failure(error);
  }
}

export async function session(request: NextRequest) {
  try {
    return json({ success: true, data: await current(accessToken(request)) });
  } catch (error) {
    return failure(error);
  }
}

export async function refresh(request: NextRequest) {
  try {
    assertSameOrigin(request);
    const token = request.cookies.get(cookieNames.access)?.value;
    if (token) {
      try {
        return json({ success: true, data: await current(token) });
      } catch (error) {
        if (!(error instanceof ApiError) || error.status !== 401) throw error;
      }
    }
    const refreshToken = request.cookies.get(cookieNames.refresh)?.value;
    if (!refreshToken || refreshToken.length > 8192)
      throw new ApiError("AUTH_001", 401);
    const result = envelope(authSchema).safeParse(
      await backendRequest("/api/v1/auth/refresh", {
        method: "POST",
        body: { refreshToken },
      }),
    );
    if (!result.success) throw new ApiError("INVALID_RESPONSE", 502);
    const auth = result.data.data;
    try {
      if (auth.user.role !== "ADMIN") throw new ApiError("AUTH_002", 403);

      return setSession(
        json({ success: true, data: await current(auth.accessToken) }),
        auth,
      );
    } catch (error) {
      try {
        await revoke(auth.refreshToken);
      } catch {
        console.warn(
          "Admin refresh cleanup could not revoke the new upstream session.",
        );
      }
      throw error;
    }
  } catch (error) {
    const response = failure(error);

    return error instanceof ApiError &&
      [401, 403].includes(error.status) &&
      error.code !== "CSRF"
      ? clearSession(response)
      : response;
  }
}

export async function logout(request: NextRequest) {
  try {
    assertSameOrigin(request);
    const token = request.cookies.get(cookieNames.refresh)?.value;
    let revoked = true;
    if (token) {
      try {
        await revoke(token);
      } catch {
        revoked = false;
      }
    }

    return clearSession(json({ success: true, data: { revoked } }));
  } catch (error) {
    return failure(error);
  }
}

export async function listAccounts(request: NextRequest) {
  try {
    const input = request.nextUrl.searchParams;
    const q = (input.get("q") ?? "").trim();
    const page = Number(input.get("page") ?? "0");
    const size = Number(input.get("size") ?? "20");
    if (
      q.length > 120 ||
      !Number.isSafeInteger(page) ||
      page < 0 ||
      page > 1000000 ||
      ![20, 50, 100].includes(size)
    )
      throw new ApiError("COMMON_003", 400);

    const query = new URLSearchParams({
      q,
      page: String(page),
      size: String(size),
    });

    const result = envelope(accountPageSchema).safeParse(
      await backendRequest(`/api/v1/admin/users?${query}`, {
        token: accessToken(request),
      }),
    );
    if (!result.success) throw new ApiError("INVALID_RESPONSE", 502);

    return json({ success: true, data: result.data.data });
  } catch (error) {
    return failure(error);
  }
}

export async function changeAccount(request: NextRequest, id: string) {
  try {
    assertSameOrigin(request);
    if (!idSchema.safeParse(id).success) throw new ApiError("COMMON_003", 400);
    const result = changeAccountSchema.safeParse(await readBody(request));
    if (!result.success) throw new ApiError("COMMON_003", 400);
    await backendRequest(`/api/v1/admin/users/${id}`, {
      method: "PATCH",
      token: accessToken(request),
      body: result.data,
    });

    return json({ success: true });
  } catch (error) {
    return failure(error);
  }
}
