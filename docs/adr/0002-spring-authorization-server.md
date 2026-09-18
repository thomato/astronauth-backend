# Spring Authorization Server for OAuth2/OIDC, used directly

OAuth2 and OpenID Connect are implemented with Spring Authorization Server. Its model (registered clients, authorizations, tokens, consent) is used and configured directly, not wrapped in our own aggregates and mappers: it is already a well-defined model, and wrapping it would add translation code without protecting any rules of our own. ADR 0001 applies to our own domain code, not to this.
