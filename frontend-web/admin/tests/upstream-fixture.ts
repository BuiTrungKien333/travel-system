// Test-only upstream. Binds loopback and is never imported by application code.
import { createServer } from "node:http";
import type { Account } from "../src/lib/contracts";
let sequence = 0;
let mongoAvailable = true;
const accounts: Account[] = [];
const access = new Map<string, string>();
const refresh = new Map<string, string>();
function reset() {
  mongoAvailable = true;
  accounts.length = 0;
  access.clear();
  refresh.clear();
  sequence = 0;
  for (let i = 1; i <= 26; i++)
    accounts.push({
      id: `a0000000-0000-0000-0000-${String(i).padStart(12, "0")}`,
      fullName:
        i === 1
          ? "Nguyễn Minh Anh"
          : i === 2
            ? "Trần Hoàng Nam"
            : `Tài khoản thử nghiệm ${i}`,
      email:
        i === 1
          ? "admin@wego.test"
          : i === 2
            ? "member@wego.test"
            : `member${i}@wego.test`,
      role: i === 1 ? "ADMIN" : i === 3 ? "GUEST" : "USER",
      locked: false,
      verified: i !== 4,
      version: 0,
      createdAt: "2026-11-01T08:00:00Z",
    });
}
reset();
function issue(user: Account) {
  const token = `test-access-${++sequence}`;
  const rt = `test-refresh-${sequence}`;
  access.set(token, user.id);
  refresh.set(rt, user.id);
  return {
    accessToken: token,
    refreshToken: rt,
    expiresIn: 1800000,
    user: {
      id: user.id,
      fullName: user.fullName,
      email: user.email,
      role: user.role,
    },
  };
}
createServer(async (req, res) => {
  res.setHeader("Content-Type", "application/json");
  const send = (status: number, data: unknown) => {
    res.writeHead(status);
    res.end(JSON.stringify(data));
  };
  const ok = (data: unknown) => send(200, { success: true, data });
  const fail = (status: number, code: string) => send(status, { code });
  const url = new URL(req.url ?? "/", "http://127.0.0.1:19091");
  const chunks: Buffer[] = [];
  for await (const chunk of req) chunks.push(Buffer.from(chunk));
  const text = Buffer.concat(chunks).toString();
  const body = text ? (JSON.parse(text) as Record<string, unknown>) : {};
  if (url.pathname === "/__test/mongo-down") {
    mongoAvailable = false;
    return ok({});
  }
  if (url.pathname === "/__test/mongo-up") {
    mongoAvailable = true;
    return ok({});
  }
  if (url.pathname === "/health") return ok({});
  if (url.pathname === "/__test/reset") {
    reset();
    return ok({});
  }
  if (url.pathname === "/__test/expire-access") {
    access.clear();
    return ok({});
  }
  if (url.pathname === "/__test/revoke-all") {
    access.clear();
    refresh.clear();
    return ok({});
  }
  if (url.pathname === "/__test/conflict") {
    const account = accounts.find((a) => a.email === body.email);
    if (account) account.version++;
    return ok({});
  }
  if (url.pathname === "/api/v1/auth/login") {
    const user = accounts.find((a) => a.email === body.email);
    if (!user || body.password !== "AdminTest123!")
      return fail(401, "AUTH_004");
    if (user.locked) return fail(403, "AUTH_007");
    return ok(issue(user));
  }
  if (url.pathname === "/api/v1/auth/refresh") {
    const id = refresh.get(String(body.refreshToken));
    refresh.delete(String(body.refreshToken));
    const user = accounts.find((a) => a.id === id);
    if (!user) return fail(401, "AUTH_005");
    if (user.locked) return fail(403, "AUTH_007");
    return ok(issue(user));
  }
  if (url.pathname === "/api/v1/auth/logout") {
    refresh.delete(String(body.refreshToken));
    res.writeHead(204);
    return res.end();
  }
  const id = access.get(
    req.headers.authorization?.replace("Bearer ", "") ?? "",
  );
  const actor = accounts.find((a) => a.id === id);
  if (!actor) return fail(401, "AUTH_001");
  if (actor.locked) return fail(401, "AUTH_001");
  if (actor.role !== "ADMIN") return fail(403, "AUTH_002");
  if (url.pathname === "/api/v1/admin/session")
    return ok({
      id: actor.id,
      fullName: actor.fullName,
      email: actor.email,
      role: actor.role,
      permissions: [
        "admin:users",
        "admin:catalog",
        "admin:ai:index",
        "admin:system:read",
      ],
    });
  if (url.pathname === "/api/v1/admin/ai/executions")
    return ok({
      status: mongoAvailable ? "AVAILABLE" : "UNAVAILABLE",
      asOf: "2026-11-17T08:00:00Z",
      days: Number(url.searchParams.get("days") ?? 30),
      page: 0,
      size: 20,
      retentionDays: 90,
      sync: {
        pending: mongoAvailable ? 0 : 1,
        retried: mongoAvailable ? 0 : 1,
      },
      data: mongoAvailable
        ? {
            total: 1,
            p50Ms: 1200,
            p95Ms: 1200,
            statuses: [{ name: "READY_WITH_WARNINGS", value: 1 }],
            runs: [
              {
                id: "fixture-run-01",
                status: "READY_WITH_WARNINGS",
                mode: "AI_ASSISTED",
                startedAt: "2026-11-17T08:00:00Z",
                durationMs: 1200,
                stages: [
                  {
                    stage: "INTENT",
                    source: "AI",
                    model: "gemini-test",
                    method: "GEMINI_STRUCTURED_EXTRACTION",
                    attempts: 1,
                    retries: 0,
                    elapsedMillis: 300,
                  },
                ],
              },
            ],
          }
        : null,
    });
  if (url.pathname === "/api/v1/admin/overview") {
    const days = Number(url.searchParams.get("days") ?? 30);
    return ok({
      asOf: "2026-11-17T08:00:00Z",
      days,
      users: 26,
      newUsers: 4,
      newTrips: 0,
      drafts: 0,
      pendingPlaces: 1,
      trend: [{ name: "2026-11-17", users: 4, trips: 0 }],
      tripStatuses: [],
      topPlaces: [],
      topProvinces: [],
      tags: [],
      rag: [],
    });
  }
  if (url.pathname === "/api/v1/admin/knowledge")
    return ok({
      content: [
        {
          id: accounts[1].id,
          name: "Bảo tàng thử nghiệm",
          province: "Đà Nẵng",
          tags: ["HISTORY"],
          sourceUrl: "https://example.com",
          verificationScope: "EXISTENCE_ONLY",
          approved: false,
          version: 1,
          indexStatus: "MISSING",
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      model: "test-embedding",
      indexAvailable: true,
    });
  if (
    url.pathname.startsWith("/api/v1/admin/catalog/") &&
    req.method === "PATCH"
  )
    return ok(null);
  if (url.pathname === "/api/v1/admin/planning/retrieval/index")
    return ok({ updated: 1, skipped: 0, failed: 0 });
  if (url.pathname === "/api/v1/admin/users") {
    const q = (url.searchParams.get("q") ?? "").toLowerCase();
    const page = Number(url.searchParams.get("page") ?? 0);
    const size = Number(url.searchParams.get("size") ?? 20);
    const filtered = accounts.filter((a) =>
      `${a.fullName} ${a.email}`.toLowerCase().includes(q),
    );
    return ok({
      content: filtered.slice(page * size, (page + 1) * size),
      page,
      size,
      totalElements: filtered.length,
    });
  }
  if (
    req.method === "PATCH" &&
    url.pathname.startsWith("/api/v1/admin/users/")
  ) {
    const target = accounts.find(
      (a) => a.id === url.pathname.split("/").at(-1),
    );
    if (!target) return fail(404, "AUTH_006");
    if (target.id === actor.id) return fail(409, "ADMIN_002");
    if (target.version !== body.version) return fail(409, "ADMIN_001");
    if (body.role === "ADMIN" && !target.verified)
      return fail(409, "ADMIN_004");
    target.role = body.role as Account["role"];
    target.locked = body.locked === true;
    target.version++;
    for (const [key, value] of access)
      if (value === target.id) access.delete(key);
    for (const [key, value] of refresh)
      if (value === target.id) refresh.delete(key);
    return ok(null);
  }
  fail(404, "NOT_FOUND");
}).listen(19091, "127.0.0.1");
