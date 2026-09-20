import { screen } from '@testing-library/react';
import { graphql, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import { renderAt, stubNavigation } from '../test/render';
import { server } from '../test/server';

function signInAnswers(result: Record<string, unknown>) {
  server.use(graphql.mutation('SignIn', () => HttpResponse.json({ data: { signIn: result } })));
}

async function signIn(user: ReturnType<typeof renderAt>['user'], email = 'ada@example.com', password = 'a password') {
  await user.type(screen.getByLabelText('Email address'), email);
  await user.type(screen.getByLabelText('Password'), password);
  await user.click(screen.getByRole('button', { name: 'Sign in' }));
}

describe('Sign-in', () => {
  let assign: ReturnType<typeof stubNavigation>;

  beforeEach(() => {
    assign = stubNavigation();
  });

  it('goes where the server says once the Credential is proven', async () => {
    signInAnswers({ __typename: 'SignedIn', email: 'ada@example.com', continueTo: '/account' });
    const { user } = renderAt('/sign-in');

    await signIn(user);

    expect(assign).toHaveBeenCalledWith('/account');
  });

  it('never says whether the address has an account', async () => {
    signInAnswers({ __typename: 'CredentialNotProven', proven: false });
    const { user } = renderAt('/sign-in');

    await signIn(user);

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('That email address and password don’t go together.');
    expect(alert).not.toHaveTextContent(/account/i);
    expect(screen.getByLabelText('Password')).toHaveValue('');
  });

  it('sends an unverified Account to register again, with the address filled in', async () => {
    signInAnswers({ __typename: 'EmailNotVerified', email: 'ada@example.com' });
    const { user } = renderAt('/sign-in');

    await signIn(user);

    expect(await screen.findByRole('heading', { name: 'Verify your email address first' })).toHaveFocus();
    expect(screen.getByRole('link', { name: 'Send a new link' })).toHaveAttribute(
      'href',
      '/register?email=ada%40example.com',
    );
  });

  it('says how long to wait when throttled', async () => {
    signInAnswers({ __typename: 'SignInThrottled', retryAfterSeconds: 120 });
    const { user } = renderAt('/sign-in');

    await signIn(user);

    expect(await screen.findByRole('alert')).toHaveTextContent('Try again in 2 minutes.');
  });

  it('asks for both fields before sending anything', async () => {
    const { user } = renderAt('/sign-in');

    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByText('Enter your email address.')).toBeInTheDocument();
    expect(screen.getByText('Enter your password.')).toBeInTheDocument();
  });
});
