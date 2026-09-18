# First-party UI is a React SPA served by Spring Boot, not Next.js

Astronauth's own pages (sign-in, Registration, email verification, consent, later passkeys and administration) are a React single-page app built with Vite, living in `frontend/`, bundled into the Spring Boot jar by Gradle and served by Spring from the same origin as the GraphQL API and the OAuth2 endpoints. An authorization server's UI must share an origin with the server that owns the session: Spring Authorization Server keeps the signed-in session in a cookie on its own origin, CSRF protection relies on it, and passkeys are bound to the page's domain. Spring already is the server for all of that, so a second server tier would only add work. Client applications never host these pages; they redirect to Astronauth.

## Considered Options

- **Next.js as a separate server behind a reverse proxy:** full Next feature set, but a second runtime that duplicates Spring's role and has to forward session cookies and CSRF tokens; its SSR and SEO benefits don't apply to a handful of pages reached by redirect.
- **Next.js static export served by Spring:** same-origin like the chosen option, but gives up SSR, server actions and middleware, leaving a heavier router than needed.
- **Client applications building their own Registration and sign-in forms against the API:** every client would handle passwords, defeating the point of OAuth2/OIDC, and passkeys could not be registered for Astronauth's domain.
