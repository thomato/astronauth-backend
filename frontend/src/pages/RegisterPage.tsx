import { useState, type FormEvent } from 'react';
import { Alert } from '../components/Alert';
import { Button } from '../components/Button';
import { PasswordField, TextField } from '../components/Field';
import { Screen } from '../components/Screen';
import { useCopy } from '../copy/useCopy';
import { graphqlRequest } from '../graphql/client';
import type { RegistrationField, RequestRegistrationMutation } from '../graphql/generated/graphql';
import { RequestRegistration } from '../graphql/operations';

type Result = RequestRegistrationMutation['requestRegistration'];
type Violation = Extract<Result, { __typename: 'RegistrationRequestRejected' }>['violations'][number];

interface Editing {
  step: 'form';
  violations: Violation[];
  missing: RegistrationField[];
  problem?: 'throttled' | 'failed';
  retryAfter?: number;
}

type State = Editing | { step: 'submitting' } | { step: 'checkInbox'; email: string };

const editing: Editing = { step: 'form', violations: [], missing: [] };

/**
 * Registration. The answer never depends on whether the address already has an Account (ADR 0003), so neither
 * does this page: after an accepted request it can only ever say "check your inbox".
 */
export function RegisterPage() {
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
      const { requestRegistration: result } = await graphqlRequest(RequestRegistration, { input: { email, password } });
      switch (result.__typename) {
        case 'RegistrationRequestAccepted':
          setState({ step: 'checkInbox', email: result.email });
          break;
        case 'RegistrationRequestRejected':
          setState({ ...editing, violations: result.violations });
          break;
        case 'RegistrationRequestThrottled':
          setState({ ...editing, problem: 'throttled', retryAfter: result.retryAfterSeconds });
          break;
      }
    } catch {
      setState({ ...editing, problem: 'failed' });
    } finally {
      // Never keep a password around longer than the request that needed it
      setPassword('');
    }
  }

  if (state.step === 'checkInbox') {
    return (
      <Screen title={copy.checkInbox.title}>
        <p className="mb-3">
          {copy.checkInbox.sentTo} <strong className="font-semibold break-all">{state.email}</strong>.
        </p>
        <p className="text-ink-muted mb-3">{copy.checkInbox.nextStep}</p>
        <p className="text-ink-muted mb-6">{copy.checkInbox.spam}</p>
        <Button variant="secondary" onClick={() => setState(editing)}>
          {copy.checkInbox.differentAddress}
        </Button>
      </Screen>
    );
  }

  const submitting = state.step === 'submitting';
  const errorsFor = (field: RegistrationField) =>
    state.step === 'form'
      ? [
          ...(state.missing.includes(field)
            ? [field === 'EMAIL' ? copy.common.required.email : copy.common.required.password]
            : []),
          ...state.violations
            .filter((v) => v.field === field)
            .map((v) => copy.register.violations[v.code](v.limit ?? null)),
        ]
      : [];

  return (
    <Screen title={copy.register.title}>
      {state.step === 'form' && state.problem === 'throttled' && (
        <Alert tone="danger">{copy.common.tryAgainIn(state.retryAfter ?? 60)}</Alert>
      )}
      {state.step === 'form' && state.problem === 'failed' && (
        <Alert tone="danger">{copy.common.somethingWentWrong}</Alert>
      )}
      <form noValidate onSubmit={submit}>
        <TextField
          label={copy.register.emailLabel}
          type="email"
          autoComplete="email"
          inputMode="email"
          spellCheck={false}
          value={email}
          onChange={setEmail}
          errors={errorsFor('EMAIL')}
        />
        <PasswordField
          label={copy.register.passwordLabel}
          autoComplete="new-password"
          value={password}
          onChange={setPassword}
          hint={copy.register.passwordHint}
          errors={errorsFor('PASSWORD')}
        />
        <Button type="submit" disabled={submitting} className="mt-2">
          {submitting ? copy.register.submitting : copy.register.submit}
        </Button>
      </form>
    </Screen>
  );
}
