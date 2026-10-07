import { useEffect, useState } from 'react';
import { Alert } from '../components/Alert';
import { Button } from '../components/Button';
import { Screen } from '../components/Screen';
import { useCopy } from '../copy/useCopy';
import { graphqlRequest } from '../graphql/client';
import { Me, SignOut } from '../graphql/operations';

type Account = { email: string; hasVerifiedEmail: boolean };

type State = { step: 'loading' } | { step: 'ready'; account: Account } | { step: 'signingOut' };

/**
 * The Account page. Spring refuses to serve it to anyone without a Session and bounces them to sign in, so
 * reaching it at all is the proof that signing in worked; me() only says who. A Session that dies while the
 * page is open shows up as a null me, which sends the browser back to the sign-in page.
 */
export function AccountPage() {
  const copy = useCopy();
  const [state, setState] = useState<State>({ step: 'loading' });

  useEffect(() => {
    let current = true;
    graphqlRequest(Me, {})
      .then(({ me }) => {
        if (!current) return;
        if (me) setState({ step: 'ready', account: me });
        else window.location.assign('/sign-in');
      })
      .catch(() => current && window.location.assign('/sign-in'));
    return () => {
      current = false;
    };
  }, []);

  async function signOut() {
    setState({ step: 'signingOut' });
    try {
      await graphqlRequest(SignOut, {});
    } finally {
      // Whatever the server said, this browser is done with the Session
      window.location.assign('/sign-in');
    }
  }

  if (state.step === 'loading') return <Screen title={copy.account.title}>{null}</Screen>;

  return (
    <Screen title={copy.account.title}>
      {state.step === 'ready' && !state.account.hasVerifiedEmail && (
        <Alert tone="danger">{copy.account.unverified}</Alert>
      )}
      {state.step === 'ready' && (
        <p className="mb-6">
          {copy.account.signedInAs} <strong className="font-semibold break-all">{state.account.email}</strong>.
        </p>
      )}
      <Button variant="secondary" onClick={signOut} disabled={state.step === 'signingOut'}>
        {state.step === 'signingOut' ? copy.account.signingOut : copy.account.signOut}
      </Button>
    </Screen>
  );
}
