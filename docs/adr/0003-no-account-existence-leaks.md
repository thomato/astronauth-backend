# Registration and sign-in never reveal whether an Account exists

An authorization server that tells strangers which email addresses have Accounts hands attackers a target list, so neither the response nor its timing may depend on whether an Account exists. Registration therefore splits in two: the request hashes the password, hands a Registration request to a queue behind an outbound port, and always returns the same neutral response without looking up the email address; a background listener later creates the Account or notifies the existing owner. The password is hashed in the request even when the Registration request will turn out to be for an existing Account and the hash is discarded; this is deliberate, and sign-in follows the same rule by hashing a dummy password when no Account matches.

## Considered Options

- **Explicit "email already registered" error:** best UX, but leaks existence outright.
- **Synchronous processing with equal work on both paths:** leaves small timing differences and invites someone to "optimise" the discarded hash away.
- **Padding every response to a fixed minimum time:** adds latency to every request and only works if the floor exceeds the slowest path.
- **Durable, Postgres-backed queue:** deferred. The first adapter is an in-memory asynchronous listener; losing a Registration request on a crash is accepted (the person registers again). Because the queue is an outbound port, a durable adapter can replace it without touching the domain.

## Consequences

- The listener must run off the request thread; Spring's default synchronous event dispatch would reintroduce the timing leak.
- The Account does not exist yet when the response is returned.
- Validation that does not depend on other Accounts (email format, password rules) still fails synchronously.
