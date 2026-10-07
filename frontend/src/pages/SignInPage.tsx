import { useState, type FormEvent } from 'react';
import { Link } from 'react-router';
import { Alert } from '../components/Alert';
import { Button, ButtonLink } from '../components/Button';
import { PasswordField, TextField } from '../components/Field';
import { Screen } from '../components/Screen';
import { useCopy } from '../copy/useCopy';
import { graphqlRequest } from '../graphql/client';
import { SignIn } from '../graphql/operations';

type Problem = 'wrongCredential' | 'throttled' | 'failed';

interface Editing {
  step: 'form';
  missing: ('EMAIL' | 'PASSWORD')[];
  problem?: Problem;
  retryAfter?: number;
}

type State = Editing | { step: 'submitting' } | { step: 'unverified'; email: string };

const editing: Editing = { step: 'form', missing: [] };

/**
 * Sign-in. A wrong password and an address with no Account are one and the same answer here, because the
 * server gives only one (ADR 0003). Where to go afterwards comes from the server, never from the URL.
 */
export function SignInPage() {
  const copy = useCopy();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [state, setState] = useState<State>(editing);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (state.step === 'submitting') return;

    const missing = [...(email.trim() ? [] : ['EMAIL' as const]), ...(password ? [] : ['PASSWORD' as const])];
    if (missing.length > 0) {
      setState({ ...editing, missing });
      return;
    }

    setState({ step: 'submitting' });
    try {
      const { signIn: result } = await graphqlRequest(SignIn, { input: { email, password } });
      switch (result.__typename) {
        case 'SignedIn':
          // A full navigation, not a route change: the destination may be an OAuth2 endpoint Spring owns
          window.location.assign(result.continueTo);
          break;
        case 'EmailNotVerified':
          setState({ step: 'unverified', email: result.email });
          break;
        case 'SignInThrottled':
          setState({ ...editing, problem: 'throttled', retryAfter: result.retryAfterSeconds });
          break;
        default:
          setState({ ...editing, problem: 'wrongCredential' });
      }
    } catch {
      setState({ ...editing, problem: 'failed' });
    } finally {
      setPassword('');
    }
  }

  if (state.step === 'unverified') {
    return (
      <Screen title={copy.signIn.unverifiedTitle}>
        <p className="text-ink-muted mb-6">{copy.signIn.unverifiedBody(state.email)}</p>
        {/* Registering again is what sends a fresh link; there is no second way to ask for one */}
        <ButtonLink to={`/register?email=${encodeURIComponent(state.email)}`}>
          {copy.signIn.unverifiedAction}
        </ButtonLink>
      </Screen>
    );
  }

  const submitting = state.step === 'submitting';
  const errorsFor = (field: 'EMAIL' | 'PASSWORD') =>
    state.step === 'form' && state.missing.includes(field)
      ? [field === 'EMAIL' ? copy.common.required.email : copy.common.required.password]
      : [];

  return (
    <Screen title={copy.signIn.title}>
      {state.step === 'form' && state.problem === 'wrongCredential' && (
        <Alert tone="danger">{copy.signIn.wrongCredential}</Alert>
      )}
      {state.step === 'form' && state.problem === 'throttled' && (
        <Alert tone="danger">{copy.common.tryAgainIn(state.retryAfter ?? 60)}</Alert>
      )}
      {state.step === 'form' && state.problem === 'failed' && (
        <Alert tone="danger">{copy.common.somethingWentWrong}</Alert>
      )}
      <form noValidate onSubmit={submit}>
        <TextField
          label={copy.signIn.emailLabel}
          type="email"
          autoComplete="username"
          inputMode="email"
          spellCheck={false}
          value={email}
          onChange={setEmail}
          errors={errorsFor('EMAIL')}
        />
        <PasswordField
          label={copy.signIn.passwordLabel}
          autoComplete="current-password"
          value={password}
          onChange={setPassword}
          errors={errorsFor('PASSWORD')}
        />
        <Button type="submit" disabled={submitting} className="mt-2">
          {submitting ? copy.signIn.submitting : copy.signIn.submit}
        </Button>
      </form>
      <p className="text-ink-muted mt-6 text-sm">
        {copy.signIn.noAccount}{' '}
        <Link to="/register" className="text-accent font-semibold hover:underline">
          {copy.signIn.register}
        </Link>
      </p>
    </Screen>
  );
}
