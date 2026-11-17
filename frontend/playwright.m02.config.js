// Shared config for the M02 relative UI re-run.
// Run from frontend/: npx playwright test -c playwright.m02.config.js tests/m02/agent-a.spec.js
const { defineConfig, devices } = require('@playwright/test');
const path = require('path');

const agent = (process.env.M02_AGENT || 'shared').toLowerCase();

module.exports = defineConfig({
  testDir: './tests/m02',
  timeout: 180_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['line'], ['json', { outputFile: path.join(__dirname, '..', 'docs/bmad/testing/evidence/relative', `playwright-raw-${agent}.json`) }]],
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3000',
    viewport: { width: 1440, height: 1000 },
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off',
    locale: 'en-US',
    timezoneId: 'Asia/Ho_Chi_Minh',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 1000 } } },
  ],
});
