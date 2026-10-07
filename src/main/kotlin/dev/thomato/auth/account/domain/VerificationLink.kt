package dev.thomato.auth.account.domain

import java.time.Duration
import java.time.Instant

/** A single-use, expiring link sent for one Registration request and bound to its Credential; see CONTEXT.md. */
data class VerificationLink(
    val tokenHash: VerificationTokenHash,
    val accountId: AccountId,
    val credential: PasswordCredential,
    val sentAt: Instant,
    val usedAt: Instant? = null,
    val invalidatedAt: Instant? = null,
    val wrongCredentialAttempts: Int = 0,
) {
    enum class Status { USABLE, EXPIRED, USED, INVALIDATED, EXHAUSTED }

    val expiresAt: Instant get() = sentAt + LIFETIME

    val attemptsLeft get() = MAX_WRONG_CREDENTIAL_ATTEMPTS - wrongCredentialAttempts

    fun status(now: Instant) =
        when {
            usedAt != null -> Status.USED
            invalidatedAt != null -> Status.INVALIDATED
            attemptsLeft <= 0 -> Status.EXHAUSTED
            !now.isBefore(expiresAt) -> Status.EXPIRED
            else -> Status.USABLE
        }

    fun recordWrongCredential() = copy(wrongCredentialAttempts = wrongCredentialAttempts + 1)

    fun use(now: Instant) = copy(usedAt = now)

    companion object {
        val LIFETIME: Duration = Duration.ofHours(24)

        /** Counted per link, not per Account, so nobody can use up another person's attempts (ADR 0006). */
        const val MAX_WRONG_CREDENTIAL_ATTEMPTS = 5

        fun send(
            token: VerificationToken,
            accountId: AccountId,
            credential: PasswordCredential,
            now: Instant,
        ) = VerificationLink(token.hash(), accountId, credential, now)
    }
}
