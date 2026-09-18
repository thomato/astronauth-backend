package dev.thomato.auth.account.domain

/** A person's accepted but not yet processed wish to register; see CONTEXT.md. */
data class RegistrationRequest(
    val email: EmailAddress,
    val credential: PasswordCredential,
)
