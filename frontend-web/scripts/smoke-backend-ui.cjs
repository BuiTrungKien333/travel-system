const assert = require("node:assert/strict");
const path = require("node:path");
const fs = require("node:fs");
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");

async function main() {
  assert.ok(
    process.env.API_TEST_EMAIL && process.env.API_TEST_PASSWORD,
    "Set local API test account credentials",
  );
  const browser = await chromium.launch({ headless: true, channel: "msedge" });
  const page = await browser.newPage();
  page.setDefaultTimeout(30_000);
  const errors = [];
  page.on("pageerror", (error) => errors.push(error.message));
  let tripId;
  try {
    await page.goto("http://localhost:3000/auth/login", {
      waitUntil: "domcontentloaded",
    });
    await page.locator("#email").fill(process.env.API_TEST_EMAIL);
    await page.locator("#password").fill(process.env.API_TEST_PASSWORD);
    await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
    await page.waitForURL("**/trips", { waitUntil: "domcontentloaded" });
    console.log("PASS UI: login and dashboard");
    await page
      .getByRole("main")
      .getByRole("link", { name: "Tạo chuyến đi", exact: true })
      .click();
    await page.waitForLoadState("domcontentloaded");
    const start = new Date();
    start.setUTCDate(start.getUTCDate() + 7);
    const end = new Date(start);
    end.setUTCDate(end.getUTCDate() + 1);
    await page.locator("#name").fill("UI API integration check");
    await page.locator("#destination").selectOption({ label: "Đà Nẵng" });
    await page.locator("#startDate").fill(start.toISOString().slice(0, 10));
    await page.locator("#endDate").fill(end.toISOString().slice(0, 10));
    await page.locator("button[type=submit]").click();
    await page.waitForURL(/\/trips\/[0-9a-f-]{36}\/itinerary$/, {
      waitUntil: "domcontentloaded",
    });
    tripId = new URL(page.url()).pathname.split("/")[2];
    console.log("PASS UI: create trip");
    await page.goto(`http://localhost:3000/trips/${tripId}/itinerary`, {
      waitUntil: "domcontentloaded",
    });
    const picker = page.locator("details").filter({
      has: page.getByText("Thêm địa điểm", { exact: true }),
    });
    await picker.locator("summary").click();
    await page
      .getByLabel("Ngày thêm địa điểm", { exact: true })
      .selectOption({ index: 1 });
    const firstCard = picker.locator("article").first();
    const placeName = await firstCard.locator("h4").innerText();
    assert.ok(
      await firstCard.locator("img, svg").count(),
      "Suggestion has a thumbnail or an explicit missing-photo state",
    );
    await firstCard.getByRole("button", { name: "Thêm", exact: true }).click();
    await page
      .getByTestId("itinerary-day-2")
      .getByRole("heading", { name: placeName, exact: true })
      .waitFor();
    assert.equal(
      await page
        .getByTestId("itinerary-day-1")
        .getByRole("heading", { name: placeName, exact: true })
        .count(),
      0,
    );
    assert.equal(
      await page
        .getByRole("button", { name: "Xếp vào ngày", exact: true })
        .count(),
      0,
    );
    const nav = page.getByRole("navigation");
    assert.equal(
      await nav.getByRole("link", { name: "Tổng quan", exact: true }).count(),
      0,
    );
    await nav.getByRole("link", { name: "Chỉ đường", exact: true }).click();
    await page
      .getByRole("heading", { name: "Chỉ đường", exact: true })
      .waitFor();
    await page.getByLabel("Ngày chỉ đường", { exact: true }).selectOption("1");
    assert.equal(
      await page
        .getByRole("link", { name: "Mở trong Google Maps", exact: true })
        .count(),
      0,
    );
    const navAuth = await page.evaluate(
      () => JSON.parse(localStorage.getItem("auth-storage")).state,
    );
    const tripPlacesResponse = await page.request.get(
      `http://localhost:8080/api/v1/trips/${tripId}/places`,
      { headers: { Authorization: `Bearer ${navAuth.token}` } },
    );
    const place = (await tripPlacesResponse.json()).data[0].place;
    const destination = [place.latitude, place.longitude];
    await page.context().grantPermissions(["geolocation"]);
    await page.context().setGeolocation({ latitude: 16.06, longitude: 108.22 });
    let routeCalls = 0;
    await page.route("**/api/v1/navigation/routes", async (route) => {
      const payload = route.request().postDataJSON();
      assert.equal(payload.trip.expectedPlaceIds.length, 1);
      assert.equal(payload.trip.nextStopIndex, 0);
      assert.equal(payload.vehicle, "DRIVE");
      routeCalls++;
      if (routeCalls === 1)
        return route.fulfill({
          status: 503,
          json: {
            success: false,
            code: "NAV_001",
            message: "Chưa thể tính đường. Vui lòng thử lại.",
          },
        });
      return route.fulfill({
        json: {
          success: true,
          data: {
            routeId: "test-route",
            generatedAt: new Date().toISOString(),
            vehicle: "DRIVE",
            provider: "geoapify-routing",
            distanceMeters: 2000,
            durationSeconds: 300,
            placeIds: payload.trip.expectedPlaceIds,
            waypoints: [
              payload.origin,
              { latitude: destination[0], longitude: destination[1] },
            ],
            attributions: ["Dữ liệu kiểm thử"],
            legs: [
              {
                distanceMeters: 2000,
                durationSeconds: 300,
                geometry: [
                  payload.origin,
                  { latitude: destination[0], longitude: destination[1] },
                ],
                steps: [
                  {
                    fromIndex: 0,
                    toIndex: 1,
                    distanceMeters: 2000,
                    durationSeconds: 300,
                    maneuver: { type: "Right", streets: ["Đường kiểm thử"] },
                  },
                ],
              },
            ],
          },
        },
      });
    });
    const findRoute = page.getByRole("button", {
      name: "Tìm đường từ vị trí hiện tại",
      exact: true,
    });
    await findRoute.click();
    await page
      .getByRole("alert")
      .filter({ hasText: "Chưa thể tính đường" })
      .waitFor();
    await findRoute.click();
    await page.getByText("Rẽ phải — Đường kiểm thử", { exact: true }).waitFor();
    console.log(
      "PASS UI: navigation request, unavailable service, route preview and Vietnamese instructions (API fixture)",
    );
    console.log(
      "PASS UI: day-2 one-click add, no separate scheduling step, navigation tab",
    );
    console.log("PASS UI: add place and save itinerary");
    await page.goto(`http://localhost:3000/trips/${tripId}/itinerary`, { waitUntil: "domcontentloaded" });
    await page.setViewportSize({ width: 390, height: 844 });
    await page.getByRole("button", { name: "Xem bản đồ", exact: true }).click();
    await page.locator(".mapboxgl-canvas").waitFor({ state: "visible" });
    await page.getByRole("button", { name: "Xem lịch trình", exact: true }).click();
    await page.getByRole("heading", { name: placeName, exact: true }).waitFor();
    assert.equal(await page.getByRole("heading", { name: placeName, exact: true }).evaluate(el => getComputedStyle(el).textOverflow), "clip");
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false);
    if (process.env.E2E_ARTIFACT_DIR) {
      fs.mkdirSync(process.env.E2E_ARTIFACT_DIR, { recursive: true });
      await page.screenshot({ path: path.join(process.env.E2E_ARTIFACT_DIR, "mobile-itinerary-fixed.png"), fullPage: true });
    }
    await page.setViewportSize({ width: 1440, height: 1000 });
    console.log("PASS UI: mobile map/list toggle, complete place names and no horizontal overflow");
    await page.goto(`http://localhost:3000/trips/${tripId}/settings`, {
      waitUntil: "domcontentloaded",
    });
    await page
      .getByLabel("Tên chuyến đi", { exact: true })
      .fill("UI API integration updated");
    await page
      .getByRole("button", { name: "Lưu thay đổi", exact: true })
      .click();
    await page.getByText("Đã lưu chuyến đi.", { exact: true }).waitFor();
    console.log("PASS UI: edit trip");
    await page.goto(`http://localhost:3000/trips/${tripId}/memories`, {
      waitUntil: "domcontentloaded",
    });
    await page
      .getByRole("button", { name: "Thêm kỷ niệm mới", exact: true })
      .click();
    await page.getByRole("button", { name: "Lưu kỷ niệm", exact: true }).click();
    await page.getByText("Hãy nhập ghi chú hoặc chọn ít nhất một ảnh.", { exact: true }).waitFor();
    await page.locator("textarea").fill("UI API note check");
    await page.locator('input[type="file"]').setInputFiles([path.join(__dirname, "fixtures/red.png"), path.join(__dirname, "fixtures/blue.png")]);
    await page.locator('select').last().selectOption({ index: 1 });
    const rejectCreate = async (route) => route.request().method() === "POST"
      ? route.fulfill({ status: 400, json: { success: false, code: "VALIDATION_ERROR", message: "Dữ liệu thử chưa hợp lệ" } })
      : route.continue();
    await page.route("**/api/v1/journal/memories", rejectCreate);
    await page.getByRole("button", { name: "Lưu kỷ niệm", exact: true }).click();
    await page.getByText(/Bạn có thể sửa thông tin rồi lưu lại/).waitFor();
    assert.equal(await page.locator("textarea").isEnabled(), true);
    await page.locator("textarea").fill("UI API note check corrected");
    await page.unroute("**/api/v1/journal/memories", rejectCreate);
    await page
      .getByRole("button", { name: "Thử lại", exact: true })
      .click();
    await page.locator("textarea").waitFor({ state: "hidden", timeout: 120_000 });
    console.log("PASS UI: empty form rejected, editable after 400, retry saved with two real Cloudinary images");
    await page.goto("http://localhost:3000/journal", {
      waitUntil: "domcontentloaded",
    });
    await page.getByText("Nhật ký khám phá cá nhân", { exact: true }).waitFor();
    await page
      .getByText("UI API note check", { exact: false })
      .first()
      .waitFor();
    console.log("PASS UI: live journal");
    await page.getByRole("heading", { name: "UI API note check corrected", exact: true }).first().click();
    const modal = page.getByRole("dialog", { name: "UI API note check corrected", exact: true });
    await modal.getByText("Ảnh 1 / 2", { exact: true }).waitFor();
    const cover = await modal.locator("img").getAttribute("src");
    await page.getByRole("button", { name: "Ảnh kế tiếp", exact: true }).click();
    await modal.getByText("Ảnh 2 / 2", { exact: true }).waitFor();
    assert.notEqual(await modal.locator("img").getAttribute("src"), cover);
    assert.equal(await modal.locator("img").evaluate(async img => { await img.decode(); return img.naturalWidth > 0; }), true);
    await page.getByRole("button", { name: "Đóng xem kỷ niệm", exact: true }).click();
    const ownMarker = page.locator(".leaflet-interactive").last();
    await ownMarker.click({ force: true });
    await page.locator(".leaflet-popup").waitFor();
    await page.locator(".leaflet-popup").getByRole("button", { name: /Xem .* kỷ niệm/ }).click();
    await page.getByRole("dialog").waitFor();
    await page.getByRole("button", { name: "Đóng xem kỷ niệm", exact: true }).click();
    assert.equal(await page.locator('img[src*="cartocdn"]').count(), 0);
    console.log("PASS UI: Journal photo navigation, decoded second photo and marker popup opens memories");
    await page.goto("http://localhost:3000/journey", { waitUntil: "domcontentloaded" });
    await page.getByRole("heading", { name: "Bản đồ các vùng miền", exact: true }).waitFor();
    assert.equal(new URL(page.url()).pathname, "/journey");
    console.log("PASS UI: Journey is separate from Journal");
    await page.goto("http://localhost:3000/account", {
      waitUntil: "domcontentloaded",
    });
    await page
      .getByRole("button", { name: "Lưu hồ sơ", exact: true })
      .waitFor();
    assert.deepEqual(errors, [], "No browser runtime errors");
    console.log("PASS UI: profile; no browser runtime errors");
  } finally {
    try {
      const auth = await page.evaluate(
        () => JSON.parse(localStorage.getItem("auth-storage"))?.state,
      );
      if (tripId && auth?.token) {
        const headers = { Authorization: `Bearer ${auth.token}` };
        const memories = await page.request.get(
          `http://localhost:8080/api/v1/journal/memories?tripId=${tripId}`,
          { headers },
        );
        for (const memory of (await memories.json()).data.content) {
          const response = await page.request.delete(
            `http://localhost:8080/api/v1/journal/memories?id=${memory.id}`,
            { headers },
          );
          assert.equal(response.status(), 204);
        }
        const read = await page.request.get(
          `http://localhost:8080/api/v1/trips/${tripId}`,
          { headers },
        );
        const body = await read.json();
        const deleted = await page.request.delete(
          `http://localhost:8080/api/v1/trips/${tripId}?version=${body.data.trip.version}`,
          { headers },
        );
        assert.equal(deleted.status(), 204);
        console.log("PASS UI: test data cleaned up");
      }
      if (auth?.refreshToken)
        await page.request.post("http://localhost:8080/api/v1/auth/logout", {
          data: { refreshToken: auth.refreshToken },
        });
    } finally {
      await browser.close();
    }
  }
}
main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
