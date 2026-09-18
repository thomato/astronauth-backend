package dev.thomato.auth.account.registration

import dev.thomato.auth.account.domain.EmailAddress
import dev.thomato.auth.account.domain.Password
import dev.thomato.auth.account.domain.PasswordCredential
import dev.thomato.auth.account.domain.PasswordHasher
import dev.thomato.auth.account.domain.RegistrationRequest
import dev.thomato.auth.account.domain.RegistrationRequestQueue

/**
 * Accepts a Registration request without ever looking up whether the email address belongs to an Account,
 * so neither the outcome nor its timing reveals that (ADR 0003). Deliberately has no dependency that could.
 */
class AcceptRegistrationRequest(
    private val passwordHasher: PasswordHasher,
    private val queue: RegistrationRequestQueue,
) {
    sealed interface Result {
        data object Accepted : Result
    }

    fun accept(
        email: String,
        password: String,
    ): Result {
        val credential = PasswordCredential(passwordHasher.hash(Password(password)))
        queue.submit(RegistrationRequest(EmailAddress(email), credential))
        return Result.Accepted
    }
}
