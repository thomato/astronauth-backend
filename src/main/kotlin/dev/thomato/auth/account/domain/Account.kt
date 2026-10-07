package dev.thomato.auth.account.domain

import java.time.Instant

class Account(
    val id: AccountId,
    val email: EmailAddress,
    val credential: PasswordCredential,
    val registeredAt: Instant,
    val emailVerifiedAt: Instant? = null,
) {
    val hasVerifiedEmail get() = emailVerifiedAt != null

    /** Completing Email verification: the proven Credential becomes the only one (ADR 0006). */
    fun verifyEmail(
        provenCredential: PasswordCredential,
        now: Instant,
    ) = Account(id, email, provenCredential, registeredAt, emailVerifiedAt ?: now)

    companion object {
        fun register(
            id: AccountId,
            request: RegistrationRequest,
            now: Instant,
        ) = Account(id, request.email, request.credential, now)
    }
}
