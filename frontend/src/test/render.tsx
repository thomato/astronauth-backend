import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router';
import { App } from '../App';

/** Renders the whole app at a URL, the way Spring serves it. */
export function renderAt(url: string) {
  window.history.pushState({}, '', url);
  const user = userEvent.setup();
  return { user, ...render(<App />, { wrapper: BrowserRouter }) };
}
