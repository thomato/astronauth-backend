package dev.thomato.auth.account.application.verification

import dev.thomato.auth.account.application.ClientAddress
import dev.thomato.auth.account.application.RateLimit
import dev.thomato.auth.account.domain.Accounts
import dev.thomato.auth.account.domain.EmailAddress
import dev.thomato.auth.account.domain.VerificationLink
import dev.thomato.auth.account.domain.VerificationLinks
import dev.thomato.auth.account.domain.VerificationToken
import java.time.Clock
import java.time.Duration

/**
 * Tells whoever holds a Verification link whether it can still be used, before they type a password.
 * Revealing the email address is safe: only someone who received the link at that address has the token.
 */
class LookUpVerificationLink(
    private val verificationLinks: VerificationLinks,
    private val accounts: Accounts,
    private val clientLimit: RateLimit,
    private val clock: Clock,
) {
    sealed interface Result {
        data class Found(
            val status: VerificationLink.Status,
            val email: EmailAddress,
        ) : Result

        data object Unknown : Result

        data class Throttled(
            val retryAfter: Duration,
        ) : Result
    }

    fun lookUp(
        token: String,
        client: ClientAddress,
    ): Result {
        val decision = clientLimit.tryAcquire(client.value)
        if (decision is RateLimit.Decision.Denied) return Result.Throttled(decision.retryAfter)

        val link = verificationLinks.find(VerificationToken(token).hash())
        val email = link?.let { accounts.get(it.accountId).email }
        return if (link == null || email == null) Result.Unknown else Result.Found(link.status(clock.instant()), email)
    }
}
