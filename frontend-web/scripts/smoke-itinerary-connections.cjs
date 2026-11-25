const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { randomUUID } = require("node:crypto");
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");

(async () => {
  const browser = await chromium.launch({ headless: true, channel: "msedge" });
  const page = await browser.newPage({
    viewport: { width: 1440, height: 1000 },
  });
  page.setDefaultTimeout(45000);
  let tripId,
    auth,
    unavailable = false;
  const calls = [],
    errors = [];
  page.on("pageerror", (error) => errors.push(error.message));
  try {
    await page.goto("http://localhost:3000/auth/login");
    await page.locator("#email").fill(process.env.API_TEST_EMAIL);
    await page.locator("#password").fill(process.env.API_TEST_PASSWORD);
    await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
    await page.waitForURL("**/trips");
    auth = await page.evaluate(
      () => JSON.parse(localStorage.getItem("auth-storage")).state,
    );
    const headers = { Authorization: `Bearer ${auth.token}` };
    const api = "http://localhost:8080/api/v1";
    const destinations = (
      await (
        await page.request.get(api + "/destinations?size=100", { headers })
      ).json()
    ).data.content;
    const destination = destinations.find((place) => place.name === "Đà Nẵng");
    const places = (
      await (
        await page.request.get(
          api + `/places/suggestions?provinceId=${destination.id}&size=6`,
          { headers },
        )
      ).json()
    ).data.content;
    assert.equal(places.length, 6);
    const startDate = new Date(Date.now() + 7 * 86400000)
      .toISOString()
      .slice(0, 10);
    const endDate = new Date(Date.now() + 8 * 86400000)
      .toISOString()
      .slice(0, 10);
    const created = await page.request.post(api + "/trips", {
      headers,
      data: {
        clientRequestId: randomUUID(),
        title: "Connection UI regression",
        destination: { name: destination.name, provinceId: destination.id },
        startDate,
        endDate,
      },
    });
    assert.equal(created.status(), 201);
    tripId = (await created.json()).data.trip.id;
    for (const [date, members] of [
      [startDate, places.slice(0, 3)],
      [endDate, places.slice(3, 5)],
    ]) {
      assert.equal(
        (
          await page.request.post(api + `/trips/${tripId}/places/batch`, {
            headers,
            data: { date, placeIds: members.map((place) => place.id) },
          })
        ).status(),
        200,
      );
    }
    // UI contract fixture; PostgreSQL + real Held-Karp/save/cache behavior is covered by backend integration tests.
    await page.route(
      `**/api/v1/trips/${tripId}/itinerary/connect`,
      async (route) => {
        const request = route.request().postDataJSON();
        calls.push(request);
        const itinerary = (
          await (
            await page.request.get(api + `/trips/${tripId}/itinerary`, {
              headers,
            })
          ).json()
        ).data;
        await route.fulfill({
          json: {
            success: true,
            data: {
              itinerary,
              days: itinerary.days.map((day) => {
                const legs = day.stops.slice(1).map((stop, index) => {
                  const previous = day.stops[index];
                  return {
                    distanceMeters: 1000,
                    durationSeconds: 120,
                    steps: [],
                    geometry: [
                      {
                        latitude: previous.latitude,
                        longitude: previous.longitude,
                      },
                      {
                        latitude:
                          (stop.latitude + previous.latitude) / 2 + 0.001,
                        longitude: (stop.longitude + previous.longitude) / 2,
                      },
                      { latitude: stop.latitude, longitude: stop.longitude },
                    ],
                  };
                });
                return {
                  date: day.date,
                  optimization: request.optimize
                    ? "GEOGRAPHIC_OPTIMUM"
                    : "PRESERVED",
                  routeErrorCode: unavailable ? "NAV_002" : null,
                  route: unavailable
                    ? null
                    : {
                        routeId: randomUUID(),
                        generatedAt: new Date().toISOString(),
                        vehicle: "DRIVE",
                        provider: "geoapify-routing",
                        waypoints: [],
                        placeIds: day.stops.map((stop) => stop.placeId),
                        legs,
                        distanceMeters: legs.length * 1000,
                        durationSeconds: legs.length * 120,
                        attributions: [
                          "Powered by Geoapify",
                          "© OpenStreetMap contributors",
                        ],
                      },
                };
              }),
            },
          },
        });
      },
    );
    const url = `http://localhost:3000/trips/${tripId}/itinerary`;
    await page.goto(url);
    const legend = page.getByTestId("itinerary-route-legend");
    await legend.getByText(/2(?:[.,]0)? km/).waitFor();
    assert.equal(
      calls[0].optimize,
      true,
      "Opening the tab automatically requests optimization",
    );
    await legend.getByText("Ngày 2", { exact: true }).waitFor();
    const picker = page.locator("details");
    await picker.locator("summary").click();
    await picker
      .getByLabel("Tìm địa điểm", { exact: true })
      .fill(places[5].name);
    await picker.getByRole("button", { name: "Tìm", exact: true }).click();
    await picker
      .locator("article")
      .filter({
        has: page.getByRole("heading", { name: places[5].name, exact: true }),
      })
      .getByRole("button", { name: "Thêm", exact: true })
      .click();
    await page
      .getByTestId("itinerary-day-1")
      .getByRole("heading", { name: places[5].name, exact: true })
      .waitFor();
    await legend.getByText(/3(?:[.,]0)? km/).waitFor();
    assert.equal(
      calls.at(-1).optimize,
      true,
      "New membership automatically recalculates the route",
    );
    await page
      .getByRole("status")
      .filter({ hasText: "Đang nối tuyến" })
      .waitFor({ state: "hidden" });
    const manualResponse = page.waitForResponse(
      (response) =>
        response.url().endsWith("/itinerary/connect") &&
        response.request().postDataJSON().optimize === false,
    );
    const handle = page
      .getByTestId("itinerary-day-1")
      .getByLabel("Kéo để sắp xếp")
      .first();
    await handle.scrollIntoViewIfNeeded();
    const source = await handle.boundingBox();
    const target = await page
      .getByTestId("itinerary-day-1")
      .getByLabel("Kéo để sắp xếp")
      .nth(1)
      .boundingBox();
    await page.mouse.move(
      source.x + source.width / 2,
      source.y + source.height / 2,
    );
    await page.mouse.down();
    await page.mouse.move(
      target.x + target.width / 2,
      target.y + target.height / 2,
      { steps: 15 },
    );
    await page.mouse.up();
    // The next UI request must preserve the user's manual order instead of optimizing it away.
    await manualResponse;
    assert.equal(calls.at(-1).optimize, false);
    if (process.env.E2E_ARTIFACT_DIR) {
      fs.mkdirSync(process.env.E2E_ARTIFACT_DIR, { recursive: true });
      await page.screenshot({
        path: path.join(
          process.env.E2E_ARTIFACT_DIR,
          "itinerary-connections-desktop.png",
        ),
      });
    }
    unavailable = true;
    await page.goto("http://localhost:3000/trips");
    await page.goto(url);
    await page.getByText(/Geoapify đã đạt hạn mức/).waitFor();
    await page.setViewportSize({ width: 390, height: 844 });
    await page.getByRole("button", { name: "Xem bản đồ", exact: true }).click();
    await page.locator(".mapboxgl-canvas").waitFor({ state: "visible" });
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 1,
      ),
      false,
    );
    unavailable = false;
    await page
      .getByRole("button", { name: "Tải tuyến đường", exact: true })
      .click();
    await page
      .getByText(/Geoapify đã đạt hạn mức/)
      .waitFor({ state: "hidden" });
    await page.evaluate(
      () =>
        new Promise((resolve) =>
          requestAnimationFrame(() => requestAnimationFrame(resolve)),
        ),
    );
    if (process.env.E2E_ARTIFACT_DIR)
      await page.screenshot({
        path: path.join(
          process.env.E2E_ARTIFACT_DIR,
          "itinerary-connections-mobile.png",
        ),
      });
    assert.deepEqual(errors, []);
    console.log(
      "PASS UI (API fixture): automatic connect on entry/add, separate days, manual order preserved, quota recovery, mobile map and no runtime errors",
    );
  } finally {
    if (auth && tripId) {
      const headers = { Authorization: `Bearer ${auth.token}` },
        api = "http://localhost:8080/api/v1";
      const trip = (
        await (
          await page.request.get(api + `/trips/${tripId}`, { headers })
        ).json()
      ).data.trip;
      assert.equal(
        (
          await page.request.delete(
            api + `/trips/${tripId}?version=${trip.version}`,
            { headers },
          )
        ).status(),
        204,
      );
    }
    if (auth)
      await page.request.post("http://localhost:8080/api/v1/auth/logout", {
        data: { refreshToken: auth.refreshToken },
      });
    await browser.close();
  }
})().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
