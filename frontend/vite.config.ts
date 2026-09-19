import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// Spring serves the build from the same origin as /graphql (ADR 0004); Gradle copies it into the jar.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  build: {
    outDir: '../build/frontend',
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    // Proxied, not cross-origin, so the XSRF-TOKEN cookie and the session behave as they do in production
    proxy: {
      '/graphql': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
  },
});
