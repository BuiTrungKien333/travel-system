import { afterEach, beforeEach, test } from "node:test";
import assert from "node:assert/strict";
import { apiRequest, openClientSession, signOut } from "../src/lib/client";
const original = globalThis.fetch;
beforeEach(() => openClientSession());
afterEach(() => {
  globalThis.fetch = original;
});
test("parallel expired requests share one refresh and retry after it succeeds", async () => {
  let fresh = false;
  let refreshes = 0;
  globalThis.fetch = async (input) => {
    if (String(input).endsWith("/auth/refresh")) {
      refreshes++;
      await new Promise((r) => setTimeout(r, 20));
      fresh = true;
      return Response.json({ success: true });
    }
    return fresh
      ? Response.json({ success: true })
      : Response.json({ code: "AUTH_001" }, { status: 401 });
  };
  await Promise.all([apiRequest("/users"), apiRequest("/users")]);
  assert.equal(refreshes, 1);
});
test("forbidden requests do not refresh or replay a mutation", async () => {
  let calls = 0;
  globalThis.fetch = async () => {
    calls++;
    return Response.json({ code: "AUTH_002" }, { status: 403 });
  };
  await assert.rejects(apiRequest("/users/id", { method: "PATCH", body: {} }), {
    code: "AUTH_002",
  });
  assert.equal(calls, 1);
});
test("logout invalidates responses from requests belonging to the previous session", async () => {
  let release: () => void = () => {};
  const wait = new Promise<void>((r) => {
    release = r;
  });
  globalThis.fetch = async (input) => {
    if (String(input).endsWith("logout"))
      return Response.json({ success: true, data: { revoked: true } });
    await wait;
    return Response.json({ private: true });
  };
  const pending = apiRequest("/users");
  const rejected = assert.rejects(pending);
  await signOut();
  release();
  await rejected;
});
