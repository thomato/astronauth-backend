# Email verification requires proving the Credential from its Registration request

Following a verification link is not enough: the person must also prove the Credential (today: enter the password) from the Registration request that caused the link to be sent, and completing Email verification makes that Credential the Account's only Credential and ends existing sessions. Without this, an attacker can pre-hijack an Account by registering someone else's email address with the attacker's own password and waiting for the real owner to verify it, either by re-registering (which sends a new link for the existing Account) or by clicking the unsolicited link. The extra step looks like friction a future reader might remove; it is the defence against that attack.

## Considered Options

- **Link only:** lets the real owner verify an Account whose password the attacker chose.
- **Bind the link to its Registration request's Credential without asking for it:** stops the re-registration case, not the victim clicking the attacker's link.
- **Create the Account only after Email verification:** incompatible with the `OPTIONAL` Verification policy, under which unverified Accounts may sign in.

## Consequences

- Completing Email verification proves both the email address and the Credential, so it can sign the person in; that is deferred to the sign-in slice.
