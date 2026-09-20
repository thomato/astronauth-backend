# Sign-in is a GraphQL mutation, not Spring Security's form login

Sign-in and Sign-out are GraphQL mutations against the same endpoint and CSRF mechanism as Registration and Email verification, not `formLogin()`. Sign-in has several outcomes a person must be able to tell apart — a wrong Credential, a throttled address, an Account whose email the Verification policy still requires to be verified — and a result union states them the way `CompleteEmailVerificationResult` already does, while a form login collapses them into a redirect with `?error`. Owning the response also makes ADR 0003's rule easier to hold: the wrong-Credential and no-such-Account paths are visibly the same value in one `when`, rather than two filter outcomes that happen to render alike.

## Consequences

- Spring gives nothing for free here. The use case must save the `SecurityContext` through a `SecurityContextRepository` and change the session id itself; **forgetting the second is a session-fixation hole**, so it is asserted in a test rather than left to review.
- The bounce to the sign-in page stays Spring's: the entry point still saves the original request. The mutation reads that saved request and returns where to continue, because no `SavedRequestAwareAuthenticationSuccessHandler` runs. `/oauth2/authorize` lands in the same cache, so the OAuth2 slice inherits this.
- Session creation has two callers, Sign-in and Email verification (ADR 0006), so starting a Session is an outbound port with one adapter rather than something a controller does.

## Considered Options

- **`formLogin()`:** session fixation protection, `SecurityContext` persistence and the saved-request redirect all correct without writing them, and it is what Spring Authorization Server's documentation assumes. Rejected for the response shape: the outcomes above either look identical to the person or leak through a query parameter.
- **A mutation delegating to Spring's `AuthenticationManager`:** keeps Spring's session handling and our response shape, but splits the Credential rules between an `AuthenticationProvider` and the domain, cutting across ADR 0001.
