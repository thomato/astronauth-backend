package dev.thomato.auth.account.application.verification

import dev.thomato.auth.account.application.ClientAddress
import dev.thomato.auth.account.application.PasswordHasher
import dev.thomato.auth.account.application.RateLimit
import dev.thomato.auth.account.application.Sessions
import dev.thomato.auth.account.application.UnitOfWork
import dev.thomato.auth.account.domain.Accounts
import dev.thomato.auth.account.domain.EmailAddress
import dev.thomato.auth.account.domain.Password
import dev.thomato.auth.account.domain.VerificationLink
import dev.thomato.auth.account.domain.VerificationLinks
import dev.thomato.auth.account.domain.VerificationToken
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Email verification: following a Verification link is not enough, the person must also prove the Credential
 * it is bound to, which then becomes the Account's only Credential (ADR 0006).
 */
@Suppress("LongParameterList") // one per port; grouping them would only hide what the use case depends on
class CompleteEmailVerification(
    private val verificationLinks: VerificationLinks,
    private val accounts: Accounts,
    private val passwordHasher: PasswordHasher,
    private val sessions: Sessions,
    private val unitOfWork: UnitOfWork,
    private val clientLimit: RateLimit,
    private val clock: Clock,
) {
    sealed interface Result {
        data class Verified(
            val email: EmailAddress,
        ) : Result

        data class WrongCredential(
            val attemptsLeft: Int,
        ) : Result

        /** Null status: no link has this token. */
        data class Unusable(
            val status: VerificationLink.Status?,
        ) : Result

        data class Throttled(
            val retryAfter: Duration,
        ) : Result
    }

    fun complete(
        token: String,
        password: String,
        client: ClientAddress,
    ): Result {
        val decision = clientLimit.tryAcquire(client.value)
        if (decision is RateLimit.Decision.Denied) return Result.Throttled(decision.retryAfter)

        // Locking the link keeps parallel attempts from getting past the attempt limit together
        return unitOfWork.run { completeWithLockedLink(VerificationToken(token), Password(password)) }
    }

    private fun completeWithLockedLink(
        token: VerificationToken,
        password: Password,
    ): Result {
        val now = clock.instant()
        val link = verificationLinks.findForUpdate(token.hash())
        val status = link?.status(now)
        return when {
            link == null -> Result.Unusable(null)
            status != VerificationLink.Status.USABLE -> Result.Unusable(status)
            password.isTooLong || !passwordHasher.matches(password, link.credential.hash) -> recordWrongCredential(link)
            else -> verify(link, now)
        }
    }

    private fun recordWrongCredential(link: VerificationLink): Result {
        val attempted = link.recordWrongCredential()
        verificationLinks.update(attempted)
        return if (attempted.attemptsLeft > 0) {
            Result.WrongCredential(attempted.attemptsLeft)
        } else {
            Result.Unusable(VerificationLink.Status.EXHAUSTED)
        }
    }

    private fun verify(
        link: VerificationLink,
        now: Instant,
    ): Result {
        val account = accounts.get(link.accountId).verifyEmail(link.credential, now)
        accounts.update(account)
        verificationLinks.update(link.use(now))
        verificationLinks.invalidateOthers(account.id, link.tokenHash, now)
        // The replaced Credential may have been an attacker's, and so may any session signed in with it
        sessions.endAll(account.id)
        // Only now: the email address and the Credential have both just been proven, which is more than
        // Sign-in proves, so this signs the person in (ADR 0006). Starting before endAll would end it too.
        sessions.start(account.id, now)
        return Result.Verified(account.email)
    }
}
