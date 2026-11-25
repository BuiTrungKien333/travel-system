const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { randomUUID } = require("node:crypto");

(async () => {
  const browser = await chromium.launch({ headless: true, channel: "msedge" });
  const page = await browser.newPage();
  let tripId, auth;
  try {
    await page.goto("http://localhost:3000/auth/login");
    await page.locator("#email").fill(process.env.API_TEST_EMAIL);
    await page.locator("#password").fill(process.env.API_TEST_PASSWORD);
    await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
    await page.waitForURL("**/trips");
    auth = await page.evaluate(() => JSON.parse(localStorage.getItem("auth-storage")).state);
    const headers = { Authorization: "Bearer " + auth.token };
    const manifest = JSON.parse(fs.readFileSync(path.join(__dirname, "../../backend/src/main/resources/db/catalog/place-photos.json")));
    const photo = manifest.photos.find(item => item.reference === "wikidata:Q1151254");
    const response = await page.request.get("http://localhost:8080/api/v1/places/" + photo.id, { headers });
    assert.equal(response.status(), 200);
    const place = (await response.json()).data;
    assert.equal(place.photos[0].url, photo.url);
    const start = new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10);
    const created = await page.request.post("http://localhost:8080/api/v1/trips", { headers, data: {
      clientRequestId: randomUUID(), title: "Photo rendering regression", destination: { name: "Hà Nội", provinceId: place.provinceId }, startDate: start, endDate: start,
    } });
    assert.equal(created.status(), 201);
    tripId = (await created.json()).data.trip.id;
    const added = await page.request.post(`http://localhost:8080/api/v1/trips/${tripId}/places/batch`, { headers, data: { placeIds: [photo.id], date: start } });
    assert.equal(added.status(), 200);
    await page.goto(`http://localhost:3000/trips/${tripId}/itinerary`);
    const image = page.getByRole("img", { name: place.name, exact: true }).first();
    await image.waitFor();
    await image.evaluate(async img => { await img.decode(); if (!img.naturalWidth) throw new Error("Photo failed decoding"); });
    assert.equal(await image.getAttribute("src"), photo.url);
    console.log("PASS: live Wikimedia photo from migrated metadata through itinerary thumbnail");
    await page.route("https://thumb.wikimedia.org/**", route => route.abort());
    await page.reload();
    await page.getByText("Ảnh tạm không tải được", { exact: true }).first().waitFor();
    console.log("PASS: failed photo uses an explicit illustrated fallback");
  } finally {
    if (tripId && auth) {
      const headers = { Authorization: "Bearer " + auth.token };
      const read = await page.request.get(`http://localhost:8080/api/v1/trips/${tripId}`, { headers });
      const trip = (await read.json()).data.trip;
      assert.equal((await page.request.delete(`http://localhost:8080/api/v1/trips/${tripId}?version=${trip.version}`, { headers })).status(), 204);
    }
    if (auth) await page.request.post("http://localhost:8080/api/v1/auth/logout", { data: { refreshToken: auth.refreshToken } });
    await browser.close();
  }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
