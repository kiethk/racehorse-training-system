import { defineConfig, devices } from '@playwright/test';
import path from 'path';

export default defineConfig({
  testDir: './',
  testMatch: /groom-suite\.e2e\.spec\.ts/,
  timeout: 45000,
  fullyParallel: false,
  workers: 1, // Single worker ensures clean sequential screenshots and consistent state
  retries: 0,
  reporter: [
    ['list'],
    ['html', { outputFolder: path.resolve(__dirname, 'results/report'), open: 'never' }],
  ],
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:3000',
    viewport: { width: 1280, height: 800 },
    trace: 'on-first-retry',
    screenshot: 'off',
    video: 'off',
    bypassCSP: true,
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
        viewport: { width: 1280, height: 800 },
      },
    },
  ],
  webServer: {
    command: 'npx next dev --webpack --port 3000',
    cwd: path.resolve(__dirname, '../../frontend'),
    url: 'http://localhost:3000',
    reuseExistingServer: true,
    timeout: 60000,
  },
});
