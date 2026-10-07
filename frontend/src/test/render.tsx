import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router';
import { vi } from 'vitest';
import { App } from '../App';

/** Renders the whole app at a URL, the way Spring serves it. */
export function renderAt(url: string) {
  window.history.pushState({}, '', url);
  const user = userEvent.setup();
  return { user, ...render(<App />, { wrapper: BrowserRouter }) };
}

const realLocation = window.location;
const readThrough = ['href', 'origin', 'protocol', 'host', 'hostname', 'port', 'pathname', 'search', 'hash'] as const;

/**
 * Captures the full-page navigations the pages make, which jsdom does not implement. Every other property
 * still reads through to the real location, so the router keeps seeing the URL renderAt pushed.
 */
export function stubNavigation() {
  const assign = vi.fn();
  const stub: Record<string, unknown> = { assign, replace: assign, reload: () => {} };
  for (const property of readThrough) {
    Object.defineProperty(stub, property, { enumerable: true, get: () => realLocation[property] });
  }
  Object.defineProperty(window, 'location', { configurable: true, writable: true, value: stub });
  return assign;
}
