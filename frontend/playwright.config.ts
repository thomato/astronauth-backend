import { defineConfig, devices } from '@playwright/test';

// Runs against the real application: Spring Boot with Postgres and Mailpit from compose.yaml.
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  use: {
    baseURL: 'http://localhost:8080',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'cd .. && ./gradlew bootRun',
    url: 'http://localhost:8080/register',
    reuseExistingServer: true,
    timeout: 240_000,
  },
});
