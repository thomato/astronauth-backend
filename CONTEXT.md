# Astronauth

An OAuth2/OpenID Connect authorization server. It owns the people who can sign in, the ways they prove who they are, and the client applications that rely on it.

## Language

### Accounts

**Account**:
A person known to Astronauth, identified by one stable identifier that client applications receive as the subject of their tokens. An Account outlives any single way of signing in.
_Avoid_: User, member, profile

**Credential**:
One way for a person to prove they own an Account, such as a password or a passkey. An Account has one or more Credentials; adding or removing one never changes the Account's identity.
_Avoid_: Login method, authenticator, auth method

**Registration request**:
A person's accepted but not yet processed wish to register with an email address and a first Credential. Accepting one never reveals whether that email address already belongs to an Account.
_Avoid_: Sign-up, pending registration

**Registration**:
Processing a Registration request: either creating a new Account with its first Credential, or, when the email address already belongs to an Account, notifying that Account's owner instead.
_Avoid_: Sign-up, user creation

**Verified email**:
The fact that a person has proven they control an Account's email address. It is recorded on the Account and never depends on configuration.
_Avoid_: Active, confirmed account

**Email verification**:
Proving control of an Account's email address by following a single-use link sent to that address and proving the Credential from the Registration request that caused the link to be sent. Completing it makes that Credential the Account's only Credential.
_Avoid_: Activation, confirmation

### Operation

**Operator**:
Whoever runs an Astronauth deployment and sets its configuration.
_Avoid_: Admin, administrator

**Verification policy**:
The Operator's rule for what an Account without a Verified email may do. It is applied when someone signs in, not when an Account is registered, so changing it affects every Account at once.
_Avoid_: Verification mode, activation setting
