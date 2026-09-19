import type { TypedDocumentString } from './generated/graphql';

export class GraphQLRequestError extends Error {}

const CSRF_COOKIE = 'XSRF-TOKEN';
const CSRF_HEADER = 'X-XSRF-TOKEN';

function csrfToken(): string | undefined {
  const cookie = document.cookie.split('; ').find((c) => c.startsWith(`${CSRF_COOKIE}=`));
  return cookie ? decodeURIComponent(cookie.slice(CSRF_COOKIE.length + 1)) : undefined;
}

function post(body: string): Promise<Response> {
  const token = csrfToken();
  return fetch('/graphql', {
    method: 'POST',
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...(token ? { [CSRF_HEADER]: token } : {}),
    },
    body,
  });
}

/**
 * Sends one operation with the CSRF token the server keeps in a cookie (double submit). When the page was not
 * served by Spring, as in development, the first refusal is what sets that cookie, so it retries once.
 */
export async function graphqlRequest<Result, Variables>(
  document: TypedDocumentString<Result, Variables>,
  variables: Variables,
): Promise<Result> {
  const body = JSON.stringify({ query: document.toString(), variables });
  const tokenBefore = csrfToken();
  let response = await post(body);
  if (response.status === 403 && csrfToken() !== tokenBefore) response = await post(body);
  if (!response.ok) throw new GraphQLRequestError(`GraphQL request failed with HTTP ${response.status}`);

  const json = (await response.json()) as { data?: Result; errors?: { message: string }[] };
  if (json.errors?.length || !json.data) {
    throw new GraphQLRequestError(json.errors?.map((e) => e.message).join('; ') ?? 'No data');
  }
  return json.data;
}
