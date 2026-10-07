import { screen } from '@testing-library/react';
import { graphql, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderAt } from '../test/render';
import { server } from '../test/server';

function linkIs(status: string, email: string | null = 'ada@example.com') {
  server.use(
    graphql.query('LookUpVerificationLink', () =>
      HttpResponse.json({ data: { verificationLink: { __typename: 'VerificationLink', status, email } } }),
    ),
  );
}

function verificationAnswers(result: Record<string, unknown>) {
  server.use(
    graphql.mutation('CompleteEmailVerification', () =>
      HttpResponse.json({ data: { completeEmailVerification: result } }),
    ),
  );
}

async function enterPassword(user: ReturnType<typeof renderAt>['user'], password: string) {
  await user.type(await screen.findByLabelText('Password'), password);
  await user.click(screen.getByRole('button', { name: 'Verify email address' }));
}

describe('Email verification', () => {
  it('takes the token out of the address bar and names the address being verified', async () => {
    linkIs('USABLE');
    renderAt('/verify-email?token=secret-token');

    expect(await screen.findByText('ada@example.com')).toBeInTheDocument();
    expect(window.location.search).toBe('');
  });

  it('verifies the address with the password from Registration', async () => {
    linkIs('USABLE');
    verificationAnswers({ __typename: 'EmailVerified', email: 'ada@example.com' });
    const { user } = renderAt('/verify-email?token=secret-token');

    await enterPassword(user, 'correct horse battery');

    expect(await screen.findByRole('heading', { name: 'Your email address is verified' })).toHaveFocus();
  });

  it('says how many attempts are left after a wrong password', async () => {
    linkIs('USABLE');
    verificationAnswers({ __typename: 'WrongCredential', attemptsLeft: 4 });
    const { user } = renderAt('/verify-email?token=secret-token');

    await enterPassword(user, 'not it');

    expect(await screen.findByRole('alert')).toHaveTextContent('4 attempts left.');
    expect(screen.getByLabelText('Password')).toHaveValue('');
  });

  it('offers registering again once the link is exhausted', async () => {
    linkIs('USABLE');
    verificationAnswers({ __typename: 'VerificationLinkUnusable', status: 'EXHAUSTED' });
    const { user } = renderAt('/verify-email?token=secret-token');

    await enterPassword(user, 'not it either');

    expect(await screen.findByRole('heading', { name: 'Too many wrong passwords' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Register again' })).toHaveAttribute('href', '/register');
  });

  it.each(['EXPIRED', 'USED', 'INVALIDATED', 'UNKNOWN'])('never asks for a password for a %s link', async (status) => {
    linkIs(status, status === 'UNKNOWN' ? null : 'ada@example.com');
    renderAt('/verify-email?token=secret-token');

    expect(await screen.findByRole('heading', { name: 'This link no longer works' })).toBeInTheDocument();
    expect(screen.queryByLabelText('Password')).not.toBeInTheDocument();
  });

  it('treats a visit without a token as a dead link', async () => {
    renderAt('/verify-email');

    expect(await screen.findByRole('heading', { name: 'This link no longer works' })).toBeInTheDocument();
  });
});
