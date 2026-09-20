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

    /**
     * Applied at Sign-in and never at Registration, so changing the Verification policy affects every Account
     * at once. Says nothing about whether the Credential was proven: that is asked first, because answering
     * this for an Account whose password the asker does not know would reveal that it exists (ADR 0003).
     */
    fun maySignIn(policy: VerificationPolicy) = hasVerifiedEmail || policy == VerificationPolicy.OPTIONAL

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
