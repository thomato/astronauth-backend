# Sign-in is throttled per email address, and an Account is never locked

Sign-in counts every attempt against the client address and against the email address that was typed, both as sliding windows that heal themselves. Attempts, not failures: the limit is acquired before anything is looked up, which is what keeps it from depending on whether an Account exists. There is no lockout: no number of failures makes an Account stop accepting Sign-in until someone intervenes.

This departs from ADR 0006, which counts wrong Credential attempts per Verification link precisely so that nobody can use up another person's attempts. Sign-in cannot honour that: the email address is the only thing an attacker stuffing credentials shares with the owner, and counting on the client address alone leaves a botnet unlimited. So we accept a bounded denial of service — an attacker can cost an owner one window — and buy it down by setting the per-address limit generously enough that a person who has forgotten their password never reaches it, while automation does.

The per-address counter is keyed on the canonical email and **acquired before the Account is looked up**, so an address with no Account throttles identically. Checking it afterwards, or only for Accounts that exist, would turn the throttle itself into the existence oracle ADR 0003 forbids.

## Consequences

- A hard lockout after N failures is the conventional answer and will be proposed again. It is rejected, not overlooked: it hands anyone a reliable way to lock a person out of their own Account permanently, and it can only be applied to Accounts that exist, which leaks existence.
- The limit is in-memory like the others, so counts reset on restart and are not shared between instances. Against a single-instance deployment that is the real limit; a shared adapter can replace it behind `RateLimit` without touching the use case.
