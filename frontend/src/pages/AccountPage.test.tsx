import { screen } from '@testing-library/react';
import { graphql, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderAt, stubNavigation } from '../test/render';
import { server } from '../test/server';

function signedInAs(me: { email: string; hasVerifiedEmail: boolean } | null) {
  server.use(graphql.query('Me', () => HttpResponse.json({ data: { me } })));
}

describe('The Account page', () => {
  let assign: ReturnType<typeof stubNavigation>;

  beforeEach(() => {
    assign = stubNavigation();
  });

  it('names the Account whose Session is making the request', async () => {
    signedInAs({ email: 'ada@example.com', hasVerifiedEmail: true });
    renderAt('/account');

    expect(await screen.findByText('ada@example.com')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('says so when the email address is not verified, which OPTIONAL allows', async () => {
    signedInAs({ email: 'ada@example.com', hasVerifiedEmail: false });
    renderAt('/account');

    expect(await screen.findByRole('alert')).toHaveTextContent('hasn’t been verified yet');
  });

  it('sends the browser to sign in when the Session has gone', async () => {
    signedInAs(null);
    renderAt('/account');

    await vi.waitFor(() => expect(assign).toHaveBeenCalledWith('/sign-in'));
  });

  it('signs out and leaves for the sign-in page', async () => {
    signedInAs({ email: 'ada@example.com', hasVerifiedEmail: true });
    let signedOut = false;
    server.use(
      graphql.mutation('SignOut', () => {
        signedOut = true;
        return HttpResponse.json({ data: { signOut: true } });
      }),
    );
    const { user } = renderAt('/account');

    await user.click(await screen.findByRole('button', { name: 'Sign out' }));

    await vi.waitFor(() => expect(assign).toHaveBeenCalledWith('/sign-in'));
    expect(signedOut).toBe(true);
  });
});
