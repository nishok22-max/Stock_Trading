import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  timeout: 35000,
  expect: { timeout: 12000 },
  use: {
    baseURL: process.env.FRONTEND_URL || 'http://127.0.0.1:4321',
    channel: 'chrome',
    viewport: { width: 1440, height: 1080 },
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  reporter: [['list'], ['html', { open: 'never' }]],
});
