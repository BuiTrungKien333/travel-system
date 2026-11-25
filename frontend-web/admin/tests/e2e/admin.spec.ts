import { test, expect, type Page } from "@playwright/test";
async function login(page: Page, email = "admin@wego.test") {
  await page.goto("/admin/login");
  await page.getByLabel("Email quản trị").fill(email);
  await page.getByLabel("Mật khẩu", { exact: true }).fill("AdminTest123!");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  if (email === "admin@wego.test") {
    await page.waitForURL(/dashboard/);
    await page.goto("/admin/users");
  }
}
test.beforeEach(async ({ request }) => {
  await request.post("http://127.0.0.1:19091/__test/reset");
});
test("direct protected navigation redirects to login", async ({ page }) => {
  await page.goto("/admin/users");
  await expect(page).toHaveURL(/\/admin\/login/);
  await expect(
    page.getByRole("heading", { name: "Đăng nhập quản trị" }),
  ).toBeVisible();
});
test("external html classes do not disrupt hydration or sign-in", async ({
  page,
}) => {
  const hydrationErrors: string[] = [];
  page.on("console", (message) => {
    if (
      message.type() === "error" &&
      /hydrat|server rendered|server-rendered/i.test(message.text())
    )
      hydrationErrors.push(message.text());
  });
  await page.route("**/admin/login", async (route) => {
    const response = await route.fetch();
    const html = await response.text();
    expect(html).toContain('<html lang="vi">');
    await route.fulfill({
      response,
      body: html.replace('<html lang="vi">', '<html lang="vi" class="mdl-js">'),
    });
  });

  await login(page);
  await expect(
    page.getByRole("heading", { name: "Quản lý tài khoản", exact: true }),
  ).toBeVisible();
  expect(hydrationErrors).toEqual([]);
});
for (const email of ["member@wego.test", "member3@wego.test"])
  test(`${email} cannot enter administration`, async ({ page, context }) => {
    await login(page, email);
    await expect(
      page.getByRole("alert").filter({ hasText: "Tài khoản không có quyền" }),
    ).toBeVisible();
    await expect(page).toHaveURL(/login/);
    expect(
      (await context.cookies()).some((c) => c.name.startsWith("wego_admin")),
    ).toBe(false);
  });
test("admin signs in, uses search/pagination and changes an account after confirmation", async ({
  page,
  context,
}) => {
  await login(page);
  await expect(
    page.getByRole("heading", { name: "Quản lý tài khoản", exact: true }),
  ).toBeVisible();
  const cookies = (await context.cookies()).filter((c) =>
    c.name.startsWith("wego_admin"),
  );
  expect(cookies).toHaveLength(2);
  expect(cookies.every((c) => c.httpOnly && c.sameSite === "Strict")).toBe(
    true,
  );
  expect(await page.evaluate(() => document.cookie)).not.toContain(
    "wego_admin",
  );
  await expect(
    page.getByRole("button", {
      name: "Quản lý tài khoản admin@wego.test",
      exact: true,
    }),
  ).toBeDisabled();
  await page.getByRole("button", { name: "Trang sau", exact: true }).click();
  await expect(page.getByText("Trang 2 / 2")).toBeVisible();
  await page.getByLabel("Tìm tên hoặc email").fill("member@wego.test");
  await page.getByRole("button", { name: "Tìm", exact: true }).click();
  await expect(page.getByText("1 tài khoản khớp tìm kiếm")).toBeVisible();
  await page
    .getByRole("button", {
      name: "Quản lý tài khoản member@wego.test",
      exact: true,
    })
    .click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await page.getByLabel("Khóa tài khoản", { exact: false }).check();
  await page.getByRole("button", { name: "Hủy", exact: true }).click();
  await expect(page.getByRole("dialog")).not.toBeVisible();
  await expect(
    page.getByRole("cell", { name: "Không khóa", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("button", {
      name: "Quản lý tài khoản member@wego.test",
      exact: true,
    })
    .click();
  await page.getByLabel("Khóa tài khoản", { exact: false }).check();
  await page.getByRole("button", { name: "Lưu thay đổi", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("Đã cập nhật tài khoản");
  await expect(
    page.getByRole("cell", { name: "Đã khóa", exact: true }),
  ).toBeVisible();
  await page.screenshot({
    path: "test-results/admin-accounts.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: "Đăng xuất", exact: true }).click();
  await expect(page).toHaveURL(/login/);
  expect(
    (await context.cookies()).some((c) => c.name.startsWith("wego_admin")),
  ).toBe(false);
});
test("expired access refreshes across parallel requests and reload restores the session", async ({
  page,
  request,
}) => {
  await login(page);
  await expect(page).toHaveURL(/users/);
  await expect(
    page.getByRole("heading", { name: "Quản lý tài khoản", exact: true }),
  ).toBeVisible();
  await request.post("http://127.0.0.1:19091/__test/expire-access");
  await page.reload();
  await expect(
    page.getByRole("heading", { name: "Quản lý tài khoản", exact: true }),
  ).toBeVisible();
});
test("revoked session redirects without displaying cached accounts", async ({
  page,
  request,
}) => {
  await login(page);
  await expect(
    page.getByRole("heading", { name: "Quản lý tài khoản", exact: true }),
  ).toBeVisible();
  await request.post("http://127.0.0.1:19091/__test/revoke-all");
  await page.reload();
  await expect(page).toHaveURL(/login/);
  await expect(page.getByRole("table")).not.toBeVisible();
});
test("stale account version blocks the write and requires reviewing fresh data", async ({
  page,
  request,
}) => {
  await login(page);
  await expect(
    page.getByRole("heading", { name: "Quản lý tài khoản", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("button", {
      name: "Quản lý tài khoản member@wego.test",
      exact: true,
    })
    .click();
  await request.post("http://127.0.0.1:19091/__test/conflict", {
    data: { email: "member@wego.test" },
  });
  await page.getByLabel("Khóa tài khoản", { exact: false }).check();
  await page.getByRole("button", { name: "Lưu thay đổi", exact: true }).click();
  await expect(page.getByRole("dialog").getByRole("alert")).toContainText(
    "Tài khoản đã thay đổi",
  );
  await expect(
    page.getByRole("button", { name: "Lưu thay đổi", exact: true }),
  ).toBeDisabled();
});
test("cross-origin write is rejected and narrow viewport remains usable", async ({
  page,
  request,
}) => {
  const response = await request.post("/admin/api/auth/logout", {
    headers: { Origin: "https://evil.example" },
  });
  expect(response.status()).toBe(403);
  await page.setViewportSize({ width: 390, height: 844 });
  await login(page);
  await expect(
    page.getByRole("heading", { name: "Quản lý tài khoản", exact: true }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: "test-results/admin-mobile.png",
    fullPage: true,
  });
});

test("role changes work and unverified accounts cannot be promoted", async ({
  page,
}) => {
  await login(page);
  await expect(
    page.getByRole("heading", { name: "Quản lý tài khoản", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("button", {
      name: "Quản lý tài khoản member4@wego.test",
      exact: true,
    })
    .click();
  await expect(
    page.getByRole("option", { name: "Quản trị viên", exact: true }),
  ).toBeDisabled();
  await page.getByRole("button", { name: "Hủy", exact: true }).click();
  await page
    .getByRole("button", {
      name: "Quản lý tài khoản member@wego.test",
      exact: true,
    })
    .click();
  await page.getByLabel("Vai trò mới").selectOption("GUEST");
  await page.getByRole("button", { name: "Lưu thay đổi", exact: true }).click();
  const row = page.getByRole("row").filter({ hasText: "member@wego.test" });
  await expect(row.getByText("Khách", { exact: true })).toBeVisible();
});
test("multiple tabs share refresh and logout clears both workspaces", async ({
  page,
  context,
  request,
}) => {
  await login(page);
  await expect(
    page.getByRole("heading", { name: "Quản lý tài khoản", exact: true }),
  ).toBeVisible();
  const other = await context.newPage();
  await other.goto("/admin/users");
  await expect(
    other.getByRole("heading", { name: "Quản lý tài khoản", exact: true }),
  ).toBeVisible();
  await request.post("http://127.0.0.1:19091/__test/expire-access");
  await Promise.all([page.reload(), other.reload()]);
  for (const tab of [page, other])
    await expect(
      tab.getByRole("heading", { name: "Quản lý tài khoản", exact: true }),
    ).toBeVisible();
  await page.getByRole("button", { name: "Đăng xuất", exact: true }).click();
  await expect(page).toHaveURL(/login/);
  await expect(other).toHaveURL(/login/);
  await expect(other.getByRole("table")).not.toBeVisible();
});

test("dashboard distinguishes real zeroes from sample charts and links to catalog and RAG", async ({
  page,
}) => {
  await login(page);
  await expect(
    page.getByRole("heading", { name: "Quản lý tài khoản", exact: true }),
  ).toBeVisible();
  await page.getByRole("link", { name: "Dashboard", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Dashboard", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByText("Dữ liệu mẫu", { exact: true }).first(),
  ).toBeVisible();
  await page.getByLabel("Khoảng thời gian").selectOption("7");
  await expect(
    page.getByText("Trong 7 ngày", { exact: true }).first(),
  ).toBeVisible();
  await expect(
    page.getByText("Chưa đo", { exact: true }).first(),
  ).toBeVisible();
  await page.screenshot({ path: "test-results/dashboard.png", fullPage: true });
  await page
    .getByRole("link", { name: "Quản lý địa điểm", exact: true })
    .click();
  await expect(
    page.getByText("Bảo tàng thử nghiệm", { exact: true }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Duyệt công khai", exact: true })
    .click();
  await page.getByRole("button", { name: "Xác nhận", exact: true }).click();
  await expect(
    page.getByRole("status").filter({ hasText: "Đã cập nhật" }),
  ).toBeVisible();
  await page
    .getByRole("link", { name: "Quản lý kho tri thức AI", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Cập nhật index", exact: true })
    .click();
  await expect(
    page.getByText("Xác nhận cập nhật index AI", { exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Hủy", exact: true }).click();
});

test("Mongo telemetry replaces sample AI charts and exposes an outage without fake numbers", async ({
  page,
  request,
}) => {
  await login(page);
  await page.getByRole("link", { name: "Dashboard", exact: true }).click();
  const panel = page.locator("section").filter({
    has: page.getByRole("heading", {
      name: "Kết quả tạo nháp AI",
      exact: true,
    }),
  });
  await expect(
    panel.getByText("MongoDB kết nối", { exact: true }),
  ).toBeVisible();
  await expect(
    panel.getByText("Dữ liệu mẫu", { exact: true }),
  ).not.toBeVisible();
  await page
    .getByRole("link", { name: "Quản lý kho tri thức AI", exact: true })
    .click();
  await page.getByText("READY_WITH_WARNINGS", { exact: true }).click();
  await expect(page.getByText("gemini-test", { exact: true })).toBeVisible();
  await request.post("http://127.0.0.1:19091/__test/mongo-down");
  await page
    .getByRole("button", { name: "Tải telemetry", exact: true })
    .click();
  await expect(page.getByText(/Chưa đọc được MongoDB/)).toBeVisible();
  await expect(
    page.getByText("fixture-run-01", { exact: false }),
  ).not.toBeVisible();
  await request.post("http://127.0.0.1:19091/__test/mongo-up");
  await page
    .getByRole("button", { name: "Tải telemetry", exact: true })
    .click();
  await expect(
    page.getByText("READY_WITH_WARNINGS", { exact: true }),
  ).toBeVisible();
});
