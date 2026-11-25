const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");

(async () => {
  const browser = await chromium.launch({ headless: true, channel: "msedge" });
  const page = await browser.newPage();
  page.setDefaultTimeout(45000);
  const errors = [];
  const results = [];
  page.on("pageerror", error => errors.push(error.message));
  const origin = process.env.WEB_TEST_URL || "http://localhost:3000";
  const publicRoutes = ["/", "/travel-guides", "/auth/login", "/auth/register"];
  async function check(route, viewport) {
    await page.setViewportSize(viewport);
    const response = await page.goto(origin + route, { waitUntil: "domcontentloaded" });
    assert.equal(response.status(), 200, route);
    await page.locator("h1, h2").first().waitFor();
    if (/\/trips\/[^/]+(?:\/itinerary)?$/.test(route) && !["/trips/new"].includes(route)) {
      await page.getByRole("heading", { name: "Lên lịch trình", exact: true }).waitFor();
    }
    const result = await page.evaluate(async () => {
      const images = [...document.images].filter(img => !img.closest(".leaflet-container"));
      images.forEach(img => img.loading = "eager");
      await Promise.race([Promise.all(images.map(img => img.decode().catch(() => {}))), new Promise(resolve => setTimeout(resolve, 20000))]);
      return {
        overflow: document.documentElement.scrollWidth > innerWidth + 1,
        brokenImages: images.filter(img => !img.naturalWidth).map(img => img.getAttribute("src")),
        missingAnchors: [...document.querySelectorAll('a[href^="/#"], a[href^="#"]')].map(a => a.hash.slice(1)).filter(id => id && !document.getElementById(id)),
      };
    });
    assert.equal(result.overflow, false, `${route}: horizontal overflow`);
    assert.deepEqual(result.brokenImages, [], `${route}: broken images`);
    if (route === "/") assert.deepEqual(result.missingAnchors, [], "Landing anchors resolve");
    results.push({ route, viewport: viewport.width, ...result });
    console.log(`PASS page ${viewport.width}px ${route}`);
  }
  try {
    for (const width of [1440, 390]) for (const route of publicRoutes) await check(route, { width, height: 1000 });
    await page.goto(origin + "/auth/login");
    await page.locator("#email").fill(process.env.API_TEST_EMAIL);
    await page.locator("#password").fill(process.env.API_TEST_PASSWORD);
    await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
    await page.waitForURL("**/trips");
    const auth = await page.evaluate(() => JSON.parse(localStorage.getItem("auth-storage")).state);
    const tripResponse = await page.request.get("http://localhost:8080/api/v1/trips?size=1", { headers: { Authorization: `Bearer ${auth.token}` } });
    const tripId = (await tripResponse.json()).data.content[0].id;
    const routes = ["/trips", "/trips/new", "/journal", "/memory", "/journey", "/account",
      `/trips/${tripId}`, ...["itinerary", "map", "memories", "settings"].map(suffix => `/trips/${tripId}/${suffix}`)];
    for (const width of [1440, 390]) for (const route of routes) await check(route, { width, height: 1000 });
    assert.deepEqual(errors, [], "No browser runtime errors");
    await page.request.post("http://localhost:8080/api/v1/auth/logout", { data: { refreshToken: auth.refreshToken } });
    if (process.env.E2E_ARTIFACT_DIR) {
      fs.mkdirSync(process.env.E2E_ARTIFACT_DIR, { recursive: true });
      fs.writeFileSync(path.join(process.env.E2E_ARTIFACT_DIR, "pages-after-fixes.json"), JSON.stringify({ results, errors }, null, 2));
    }
    console.log(`PASS ${results.length} page/viewport cases`);
  } finally { await browser.close(); }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
