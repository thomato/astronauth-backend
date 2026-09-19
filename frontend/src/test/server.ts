import { setupServer } from 'msw/node';

/** Mocks /graphql at the network boundary; each test installs the answers it needs with server.use(). */
export const server = setupServer();
