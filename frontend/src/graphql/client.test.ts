import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '../test/server';
import { graphqlRequest } from './client';
import { LookUpVerificationLink } from './operations';

describe('graphqlRequest', () => {
  it('retries once when the refusal hands out a new CSRF token, as when Vite served the page', async () => {
    const tokens: (string | null)[] = [];
    server.use(
      http.post('/graphql', ({ request }) => {
        tokens.push(request.headers.get('X-XSRF-TOKEN'));
        if (tokens.length === 1) {
          document.cookie = 'XSRF-TOKEN=fresh; path=/';
          return new HttpResponse(null, { status: 403 });
        }
        return HttpResponse.json({ data: { verificationLink: { __typename: 'VerificationLink', status: 'UNKNOWN' } } });
      }),
    );

    await graphqlRequest(LookUpVerificationLink, { token: 't' });

    expect(tokens).toEqual([null, 'fresh']);
  });

  it('fails on GraphQL errors instead of returning partial data', async () => {
    server.use(http.post('/graphql', () => HttpResponse.json({ errors: [{ message: 'boom' }] })));

    await expect(graphqlRequest(LookUpVerificationLink, { token: 't' })).rejects.toThrow('boom');
  });
});
