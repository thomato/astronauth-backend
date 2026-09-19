import { useEffect, useState, type FormEvent } from 'react';
import { Alert } from '../components/Alert';
import { Button, ButtonLink } from '../components/Button';
import { PasswordField } from '../components/Field';
import { Screen } from '../components/Screen';
import { useCopy } from '../copy/useCopy';
import { graphqlRequest } from '../graphql/client';
import { CompleteEmailVerification, LookUpVerificationLink } from '../graphql/operations';

type State =
  | { step: 'checking' }
  | {
      step: 'form';
      email: string;
      submitting?: boolean;
      problem?: 'wrongPassword' | 'throttled' | 'failed' | 'missing';
      attemptsLeft?: number;
      retryAfter?: number;
    }
  | { step: 'verified'; email: string }
  | { step: 'exhausted' }
  | { step: 'dead' }
  | { step: 'unavailable'; retryAfter?: number };

/** Reads the token once and takes it out of the address bar, so it stays out of history and bookmarks. */
function useTokenFromUrl(): string | null {
  const [token] = useState(() => new URLSearchParams(window.location.search).get('token'));
  useEffect(() => {
    if (token) window.history.replaceState(window.history.state, '', window.location.pathname);
  }, [token]);
  return token;
}

/**
 * Email verification (ADR 0006): following the link is not enough, the person must also enter the password
 * the link is bound to. The link's status is checked first, so nobody types a password into a dead link.
 */
export function VerifyEmailPage() {
  const copy = useCopy();
  const token = useTokenFromUrl();
  const [password, setPassword] = useState('');
  const [state, setState] = useState<State>(token ? { step: 'checking' } : { step: 'dead' });

  useEffect(() => {
    if (!token) return;
    let current = true;
    graphqlRequest(LookUpVerificationLink, { token })
      .then(({ verificationLink: result }) => {
        if (!current) return;
        if (result.__typename === 'VerificationThrottled') {
          setState({ step: 'unavailable', retryAfter: result.retryAfterSeconds });
        } else if (result.status === 'USABLE' && result.email) {
          setState({ step: 'form', email: result.email });
        } else {
          setState(result.status === 'EXHAUSTED' ? { step: 'exhausted' } : { step: 'dead' });
        }
      })
      .catch(() => current && setState({ step: 'unavailable' }));
    return () => {
      current = false;
    };
  }, [token]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (state.step !== 'form' || state.submitting || !token) return;
    const form = { step: 'form' as const, email: state.email };
    if (!password) {
      setState({ ...form, problem: 'missing' });
      return;
    }

    setState({ ...form, submitting: true });
    try {
      const { completeEmailVerification: result } = await graphqlRequest(CompleteEmailVerification, {
        input: { token, password },
      });
      switch (result.__typename) {
        case 'EmailVerified':
          setState({ step: 'verified', email: result.email });
          break;
        case 'WrongCredential':
          setState({ ...form, problem: 'wrongPassword', attemptsLeft: result.attemptsLeft });
          break;
        case 'VerificationLinkUnusable':
          setState(result.status === 'EXHAUSTED' ? { step: 'exhausted' } : { step: 'dead' });
          break;
        case 'VerificationThrottled':
          setState({ ...form, problem: 'throttled', retryAfter: result.retryAfterSeconds });
          break;
      }
    } catch {
      setState({ ...form, problem: 'failed' });
    } finally {
      setPassword('');
    }
  }

  switch (state.step) {
    case 'checking':
      return (
        <Screen title={copy.verifyEmail.title}>
          <p role="status" className="text-ink-muted">
            {copy.verifyEmail.checking}
          </p>
        </Screen>
      );
    case 'verified':
      return (
        <Screen title={copy.verifyEmail.verifiedTitle}>
          <Alert tone="success">{copy.verifyEmail.verifiedBody(state.email)}</Alert>
        </Screen>
      );
    case 'exhausted':
      return (
        <Screen title={copy.verifyEmail.exhaustedTitle}>
          <p className="text-ink-muted mb-6">{copy.verifyEmail.exhaustedBody}</p>
          <ButtonLink to="/register">{copy.verifyEmail.registerAgain}</ButtonLink>
        </Screen>
      );
    case 'dead':
      return (
        <Screen title={copy.verifyEmail.deadTitle}>
          <p className="text-ink-muted mb-6">{copy.verifyEmail.deadBody}</p>
          <ButtonLink to="/register">{copy.verifyEmail.registerAgain}</ButtonLink>
        </Screen>
      );
    case 'unavailable':
      return (
        <Screen title={copy.verifyEmail.title}>
          <Alert tone="danger">
            {state.retryAfter ? copy.common.tryAgainIn(state.retryAfter) : copy.common.somethingWentWrong}
          </Alert>
        </Screen>
      );
    case 'form':
      return (
        <Screen title={copy.verifyEmail.title}>
          {state.problem === 'wrongPassword' && (
            <Alert tone="danger">{copy.verifyEmail.wrongPassword(state.attemptsLeft ?? 0)}</Alert>
          )}
          {state.problem === 'throttled' && (
            <Alert tone="danger">{copy.common.tryAgainIn(state.retryAfter ?? 60)}</Alert>
          )}
          {state.problem === 'failed' && <Alert tone="danger">{copy.common.somethingWentWrong}</Alert>}
          <p className="mb-5">
            {copy.verifyEmail.forAddress} <strong className="font-semibold break-all">{state.email}</strong>.
          </p>
          <form noValidate onSubmit={submit}>
            {/* Lets password managers file the password under the right account */}
            <input type="email" name="username" autoComplete="username" value={state.email} readOnly hidden />
            <PasswordField
              label={copy.verifyEmail.passwordLabel}
              autoComplete="current-password"
              value={password}
              onChange={setPassword}
              hint={copy.verifyEmail.passwordHint}
              errors={state.problem === 'missing' ? [copy.common.required.password] : []}
            />
            <Button type="submit" disabled={state.submitting} className="mt-2">
              {state.submitting ? copy.verifyEmail.submitting : copy.verifyEmail.submit}
            </Button>
          </form>
        </Screen>
      );
  }
}
