import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./tests/e2e",
  fullyParallel: false,
  workers: 1,
  timeout: 45000,
  expect: { timeout: 12000 },
  use: {
    baseURL: "http://localhost:3101/admin",
    browserName: "chromium",
    channel: "msedge",
    headless: true,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  webServer: [
    {
      command: "npx tsx tests/upstream-fixture.ts",
      url: "http://127.0.0.1:19091/health",
      reuseExistingServer: false,
      timeout: 30000,
    },
    {
      command: "node node_modules/next/dist/bin/next dev --port 3101",
      url: "http://localhost:3101/admin/login",
      reuseExistingServer: false,
      timeout: 120000,
      env: {
        BACKEND_URL: "http://127.0.0.1:19091",
        ADMIN_ORIGIN: "http://localhost:3101",
      },
    },
  ],
});
