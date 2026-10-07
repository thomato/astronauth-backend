import { screen, waitFor } from '@testing-library/react';
import { graphql, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderAt } from '../test/render';
import { server } from '../test/server';

const requestRegistration = graphql.mutation;

function answer(result: Record<string, unknown>, seen?: (request: Request, variables: unknown) => void) {
  server.use(
    requestRegistration('RequestRegistration', ({ request, variables }) => {
      seen?.(request, variables);
      return HttpResponse.json({ data: { requestRegistration: result } });
    }),
  );
}

async function fillIn(user: ReturnType<typeof renderAt>['user'], email: string, password: string) {
  if (email) await user.type(screen.getByLabelText('Email address'), email);
  if (password) await user.type(screen.getByLabelText('Password'), password);
  await user.click(screen.getByRole('button', { name: 'Create account' }));
}

describe('Registration', () => {
  it('asks for both fields without sending anything when they are empty', async () => {
    const { user } = renderAt('/register');

    await fillIn(user, '', '');

    expect(screen.getByLabelText('Email address')).toHaveAccessibleDescription('Enter your email address.');
    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription(/Enter your password\./);
  });

  it('sends the CSRF token from the cookie and says to check the inbox, whatever the address', async () => {
    document.cookie = 'XSRF-TOKEN=token-from-cookie; path=/';
    let sent: { csrf: string | null; variables: unknown } | undefined;
    answer({ __typename: 'RegistrationRequestAccepted', email: 'ada@example.com' }, (request, variables) => {
      sent = { csrf: request.headers.get('X-XSRF-TOKEN'), variables };
    });
    const { user } = renderAt('/register');

    await fillIn(user, 'ada@example.com', 'correct horse battery');

    expect(await screen.findByRole('heading', { name: 'Check your inbox' })).toHaveFocus();
    expect(screen.getByText('ada@example.com')).toBeInTheDocument();
    expect(sent).toEqual({
      csrf: 'token-from-cookie',
      variables: { input: { email: 'ada@example.com', password: 'correct horse battery' } },
    });
  });

  it('goes back to the form with the address kept and the password cleared', async () => {
    answer({ __typename: 'RegistrationRequestAccepted', email: 'ada@example.com' });
    const { user } = renderAt('/register');
    await fillIn(user, 'ada@example.com', 'correct horse battery');

    await user.click(await screen.findByRole('button', { name: 'Use a different address' }));

    expect(screen.getByLabelText('Email address')).toHaveValue('ada@example.com');
    expect(screen.getByLabelText('Password')).toHaveValue('');
  });

  it('shows each broken rule at its field, worded with the limit the server sent', async () => {
    answer({
      __typename: 'RegistrationRequestRejected',
      violations: [
        { field: 'EMAIL', code: 'EMAIL_INVALID', limit: null },
        { field: 'PASSWORD', code: 'PASSWORD_TOO_SHORT', limit: 12 },
      ],
    });
    const { user } = renderAt('/register');

    await fillIn(user, 'ada', 'short');

    await waitFor(() =>
      expect(screen.getByLabelText('Email address')).toHaveAccessibleDescription(
        'Enter an email address like name@example.com.',
      ),
    );
    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription(/Use at least 12 characters\./);
    expect(screen.getByLabelText('Password')).toHaveValue('');
  });

  it('tells a throttled client how long to wait', async () => {
    answer({ __typename: 'RegistrationRequestThrottled', retryAfterSeconds: 290 });
    const { user } = renderAt('/register');

    await fillIn(user, 'ada@example.com', 'correct horse battery');

    expect(await screen.findByRole('alert')).toHaveTextContent('Too many attempts. Try again in 5 minutes.');
  });

  it('says so when the request fails', async () => {
    server.use(requestRegistration('RequestRegistration', () => HttpResponse.error()));
    const { user } = renderAt('/register');

    await fillIn(user, 'ada@example.com', 'correct horse battery');

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong');
  });

  it('can show the password so typos are caught', async () => {
    const { user } = renderAt('/register');
    await user.type(screen.getByLabelText('Password'), 'secret');

    await user.click(screen.getByRole('button', { name: 'Show password' }));

    expect(screen.getByLabelText('Password')).toHaveAttribute('type', 'text');
  });
});
