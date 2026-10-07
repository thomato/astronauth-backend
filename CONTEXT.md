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
Processing a Registration request: creating a new Account and sending a Verification link, or, when the email address already belongs to an Account, notifying that Account's owner instead: with a new Verification link while the email is not yet a Verified email, with a notice once it is.
_Avoid_: Sign-up, user creation

**Verified email**:
The fact that a person has proven they control an Account's email address. It is recorded on the Account and never depends on configuration.
_Avoid_: Active, confirmed account

**Email verification**:
Proving control of an Account's email address by following a Verification link and proving the Credential it is bound to. Completing it makes that Credential the Account's only Credential and invalidates the Account's other Verification links.
_Avoid_: Activation, confirmation

**Verification link**:
A single-use, expiring link sent to an Account's email address for one Registration request and bound to that request's Credential. An Account can have several at once; a new one never revokes older ones.
_Avoid_: Activation link, confirmation link, verification token

### Signing in

**Sign-in**:
A person proving a Credential to start a Session. It never reveals whether an Account exists, and what an Account without a Verified email may do is left to the Verification policy.
_Avoid_: Login, log in, authentication

**Session**:
A person's proven presence in one browser, started by Sign-in or by completing Email verification, holding the Account it belongs to and the moment its Credential was proven. An Account can have several Sessions at once, one per browser.
_Avoid_: Login session, token

**Sign-out**:
Ending the one Session doing the signing out. An Account's other Sessions are left alone; only replacing its Credential ends them all.
_Avoid_: Logout, log out

### Operation

**Operator**:
Whoever runs an Astronauth deployment and sets its configuration.
_Avoid_: Admin, administrator

**Verification policy**:
The Operator's rule for what an Account without a Verified email may do: under OPTIONAL it may sign in, under REQUIRED it may not. It is applied when someone signs in, not when an Account is registered, so changing it affects every Account at once.
_Avoid_: Verification mode, activation setting
