import { afterEach, beforeEach, test } from "node:test";
import assert from "node:assert/strict";
import { NextRequest } from "next/server";
import {
  login,
  logout,
  session,
  refresh,
  listAccounts,
  changeAccount,
} from "../src/server/handlers";
import { GET as telemetry } from "../src/app/api/telemetry/route";
import { backendRequest } from "../src/server/backend";
const original = globalThis.fetch;
const user = {
  id: "a0000000-0000-0000-0000-000000000001",
  fullName: "Test admin",
  email: "admin@wego.test",
  role: "ADMIN",
  permissions: ["admin:users"],
};
const auth = {
  accessToken: "private-access",
  refreshToken: "private-refresh",
  expiresIn: 1800000,
  user: { role: "ADMIN" },
};
const calls: { path: string; options?: RequestInit }[] = [];
function ok(data: unknown) {
  return Response.json({ success: true, data });
}
function mock(
  handler: (
    path: string,
    options?: RequestInit,
  ) => Response | Promise<Response>,
) {
  globalThis.fetch = async (input, options) => {
    const path =
      new URL(String(input)).pathname + new URL(String(input)).search;
    calls.push({ path, options });
    return handler(path, options);
  };
}
function request(
  path: string,
  body?: unknown,
  extra: Record<string, string> = {},
  method = "POST",
) {
  return new NextRequest(`http://localhost:3001/admin/api${path}`, {
    method,
    headers: {
      origin: "http://localhost:3001",
      ...(body !== undefined ? { "content-type": "application/json" } : {}),
      ...extra,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
}
beforeEach(() => {
  calls.length = 0;
  process.env.BACKEND_URL = "http://127.0.0.1:18081";
  process.env.ADMIN_ORIGIN = "http://localhost:3001";
});
afterEach(() => {
  globalThis.fetch = original;
});
test("cross-site login is rejected before calling backend", async () => {
  mock(() => ok(auth));
  const response = await login(
    request(
      "/auth/login",
      { email: user.email, password: "secret" },
      { origin: "https://evil.example" },
    ),
  );
  assert.equal(response.status, 403);
  assert.equal(calls.length, 0);
});
test("successful admin login puts tokens only into HttpOnly cookies", async () => {
  mock((path) => (path.endsWith("login") ? ok(auth) : ok(user)));
  const response = await login(
    request("/auth/login", { email: user.email, password: "secret" }),
  );
  assert.equal(response.status, 200);
  const text = await response.text();
  assert.ok(!text.includes("private-access"));
  assert.ok(!text.includes("private-refresh"));
  const cookies = response.headers.get("set-cookie") ?? "";
  assert.match(cookies, /HttpOnly/i);
  assert.match(cookies, /SameSite=strict/i);
  assert.match(cookies, /Path=\/admin/i);
  assert.equal(calls.length, 2);
});
test("non-admin login is rejected and the newly issued refresh token is revoked", async () => {
  mock((path) =>
    path.endsWith("logout")
      ? new Response(null, { status: 204 })
      : ok({ ...auth, user: { role: "USER" } }),
  );
  const response = await login(
    request("/auth/login", { email: "user@wego.test", password: "secret" }),
  );
  assert.equal(response.status, 403);
  assert.equal(response.headers.get("set-cookie"), null);
  assert.ok(calls.some((c) => c.path.endsWith("logout")));
});
test("session requires an access cookie and never discloses tokens", async () => {
  mock(() => ok(user));
  assert.equal(
    (await session(request("/auth/session", undefined, {}, "GET"))).status,
    401,
  );
  assert.equal(calls.length, 0);
  const response = await session(
    request(
      "/auth/session",
      undefined,
      { cookie: "wego_admin_access=private-access" },
      "GET",
    ),
  );
  assert.equal(response.status, 200);
  assert.ok(!(await response.text()).includes("private-access"));
});
test("refresh with an already valid access token does not rotate another tab's refresh token", async () => {
  mock(() => ok(user));
  const response = await refresh(
    request("/auth/refresh", undefined, {
      cookie: "wego_admin_access=current; wego_admin_refresh=current-refresh",
    }),
  );
  assert.equal(response.status, 200);
  assert.equal(calls.length, 1);
  assert.ok(calls[0].path.endsWith("session"));
  assert.equal(response.headers.get("set-cookie"), null);
});
test("expired access is refreshed and current admin capabilities are rechecked", async () => {
  mock((path) => (path.endsWith("refresh") ? ok(auth) : ok(user)));
  const response = await refresh(
    request("/auth/refresh", undefined, { cookie: "wego_admin_refresh=old" }),
  );
  assert.equal(response.status, 200);
  assert.match(response.headers.get("set-cookie") ?? "", /private-access/);
  assert.equal(calls.length, 2);
});
test("revoked refresh clears cookies but a temporary backend failure preserves the session", async () => {
  mock(() => Response.json({ code: "AUTH_005" }, { status: 401 }));
  const rejected = await refresh(
    request("/auth/refresh", undefined, {
      cookie: "wego_admin_refresh=revoked",
    }),
  );
  assert.equal(rejected.status, 401);
  assert.match(rejected.headers.get("set-cookie") ?? "", /Max-Age=0/);
  mock(() => {
    throw new Error("network");
  });
  const temporary = await refresh(
    request("/auth/refresh", undefined, {
      cookie: "wego_admin_refresh=still-valid",
    }),
  );
  assert.equal(temporary.status, 503);
  assert.equal(temporary.headers.get("set-cookie"), null);
});
test("logout clears local cookies even when upstream revocation is unavailable", async () => {
  mock(() => {
    throw new Error("network");
  });
  const response = await logout(
    request("/auth/logout", undefined, { cookie: "wego_admin_refresh=old" }),
  );
  assert.equal(response.status, 200);
  assert.equal((await response.json()).data.revoked, false);
  assert.match(response.headers.get("set-cookie") ?? "", /Max-Age=0/);
});
test("cross-site logout cannot clear a valid session", async () => {
  mock(() => ok(null));
  const response = await logout(
    request("/auth/logout", undefined, { origin: "https://evil.example" }),
  );
  assert.equal(response.status, 403);
  assert.equal(response.headers.get("set-cookie"), null);
  assert.equal(calls.length, 0);
});
test("account updates validate body and identifier and preserve expected version", async () => {
  mock(() => ok(null));
  const input = { role: "USER", locked: true, version: 7 };
  assert.equal(
    (await changeAccount(request("/users/x", input), "../auth/login")).status,
    400,
  );
  assert.equal(
    (
      await changeAccount(
        request("/users/x", { ...input, extra: "untrusted" }),
        user.id,
      )
    ).status,
    400,
  );
  assert.equal(calls.length, 0);
  const response = await changeAccount(
    request("/users/x", input, { cookie: "wego_admin_access=token" }, "PATCH"),
    user.id,
  );
  assert.equal(response.status, 200);
  assert.deepEqual(JSON.parse(String(calls[0].options?.body)), input);
});
test("version conflicts are returned without retrying the write or exposing upstream content", async () => {
  mock(() =>
    Response.json(
      { code: "ADMIN_001", message: "sensitive diagnostic" },
      { status: 409 },
    ),
  );
  const response = await changeAccount(
    request(
      "/users/x",
      { role: "USER", locked: false, version: 0 },
      { cookie: "wego_admin_access=token" },
      "PATCH",
    ),
    user.id,
  );
  assert.equal(response.status, 409);
  assert.equal(calls.length, 1);
  assert.ok(!(await response.text()).includes("sensitive diagnostic"));
});
test("list rejects invalid pagination and forwards only allowlisted fields", async () => {
  mock(() => ok({ content: [], page: 0, size: 20, totalElements: 0 }));
  assert.equal(
    (await listAccounts(request("/users?page=-1", undefined, {}, "GET")))
      .status,
    400,
  );
  assert.equal(
    (
      await listAccounts(
        request(
          "/users?page=0&inject=secret",
          undefined,
          { cookie: "wego_admin_access=token" },
          "GET",
        ),
      )
    ).status,
    200,
  );
  assert.ok(!calls[0].path.includes("inject"));
});
test("oversized request bodies and open proxy targets are rejected", async () => {
  mock(() => ok(auth));
  assert.equal(
    (
      await login(
        request("/auth/login", {
          email: user.email,
          password: "x".repeat(17000),
        }),
      )
    ).status,
    413,
  );
  assert.equal(calls.length, 0);
  await assert.rejects(backendRequest("https://evil.example/path"), {
    code: "AUTH_002",
  });
});

test("telemetry proxy validates pagination, requires cookies and preserves unavailable state", async () => {
  mock(() =>
    ok({ status: "UNAVAILABLE", days: 30, page: 0, sync: { pending: 1 } }),
  );
  assert.equal(
    (await telemetry(request("/telemetry?days=999", undefined, {}, "GET")))
      .status,
    400,
  );
  assert.equal(
    (await telemetry(request("/telemetry", undefined, {}, "GET"))).status,
    401,
  );
  const response = await telemetry(
    request(
      "/telemetry?days=7&page=2",
      undefined,
      { cookie: "wego_admin_access=token" },
      "GET",
    ),
  );
  assert.equal(response.status, 200);
  assert.equal((await response.json()).data.status, "UNAVAILABLE");
  assert.equal(calls[0].path, "/api/v1/admin/ai/executions?days=7&page=2");
});
