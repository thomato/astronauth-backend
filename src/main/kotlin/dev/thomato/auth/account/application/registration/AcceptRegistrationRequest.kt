package dev.thomato.auth.account.application.registration

import dev.thomato.auth.account.application.ClientAddress
import dev.thomato.auth.account.application.PasswordHasher
import dev.thomato.auth.account.application.RateLimit
import dev.thomato.auth.account.domain.EmailAddress
import dev.thomato.auth.account.domain.Password
import dev.thomato.auth.account.domain.PasswordCredential
import dev.thomato.auth.account.domain.RegistrationRequest
import dev.thomato.auth.account.domain.Violation
import java.time.Duration

/**
 * Accepts a Registration request without ever looking up whether the email address belongs to an Account,
 * so neither the outcome nor its timing reveals that (ADR 0003). Deliberately has no dependency that could:
 * the client limit and the rules depend only on the caller and on what they entered.
 */
class AcceptRegistrationRequest(
    private val passwordHasher: PasswordHasher,
    private val queue: RegistrationRequestQueue,
    private val clientLimit: RateLimit,
) {
    sealed interface Result {
        data class Accepted(
            val email: EmailAddress,
        ) : Result

        data class Rejected(
            val violations: List<Violation>,
        ) : Result

        data class Throttled(
            val retryAfter: Duration,
        ) : Result
    }

    fun accept(
        email: String,
        password: String,
        client: ClientAddress,
    ): Result {
        // Before anything costly, so a flood of requests cannot make Astronauth hash on its behalf
        val decision = clientLimit.tryAcquire(client.value)
        if (decision is RateLimit.Decision.Denied) return Result.Throttled(decision.retryAfter)

        val typedPassword = Password(password)
        val violations = EmailAddress.violations(email) + typedPassword.violations
        return if (violations.isEmpty()) submit(EmailAddress(email), typedPassword) else Result.Rejected(violations)
    }

    private fun submit(
        email: EmailAddress,
        password: Password,
    ): Result {
        queue.submit(RegistrationRequest(email, PasswordCredential(passwordHasher.hash(password))))
        return Result.Accepted(email)
    }
}
