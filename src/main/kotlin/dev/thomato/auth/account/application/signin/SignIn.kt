package dev.thomato.auth.account.application.signin

import dev.thomato.auth.account.application.ClientAddress
import dev.thomato.auth.account.application.PasswordHasher
import dev.thomato.auth.account.application.RateLimit
import dev.thomato.auth.account.application.Sessions
import dev.thomato.auth.account.domain.Account
import dev.thomato.auth.account.domain.Accounts
import dev.thomato.auth.account.domain.EmailAddress
import dev.thomato.auth.account.domain.Password
import dev.thomato.auth.account.domain.PasswordHash
import dev.thomato.auth.account.domain.VerificationPolicy
import java.time.Clock
import java.time.Duration
import java.util.UUID

/**
 * Sign-in: proving a Credential to start a Session. Nothing it returns, and nothing it spends time on, may
 * depend on whether the email address belongs to an Account (ADR 0003), so the order below is load-bearing:
 * limits, then the lookup, then the Credential, and only then the Verification policy.
 */
@Suppress("LongParameterList") // one per port; grouping them would only hide what the use case depends on
class SignIn(
    private val accounts: Accounts,
    private val passwordHasher: PasswordHasher,
    private val sessions: Sessions,
    private val clientLimit: RateLimit,
    private val addressLimit: RateLimit,
    private val policy: VerificationPolicy,
    private val clock: Clock,
) {
    sealed interface Result {
        data class SignedIn(
            val email: EmailAddress,
        ) : Result

        /** Also what an address with no Account gets, and for the same cost. */
        data object CredentialNotProven : Result

        data class EmailNotVerified(
            val email: EmailAddress,
        ) : Result

        data class Throttled(
            val retryAfter: Duration,
        ) : Result
    }

    /**
     * The hash an absent Account is checked against, so no Account costs the same as a wrong password
     * (ADR 0003). Hashing a password nobody knows, rather than a constant, so it can never match.
     */
    private val absentCredential: PasswordHash by lazy {
        passwordHasher.hash(Password(UUID.randomUUID().toString()))
    }

    fun attempt(
        email: String,
        password: String,
        client: ClientAddress,
    ): Result {
        // The address limit is keyed on the canonical address and acquired before the lookup, so an address
        // with no Account throttles identically (ADR 0008)
        val throttled = throttle(clientLimit, client.value) ?: throttle(addressLimit, email.trim().lowercase())
        if (throttled != null) return throttled

        val account = findAccount(email)
        val typed = Password(password)
        // Always hashes, whether or not an Account was found
        val proven = !typed.isTooLong && passwordHasher.matches(typed, account?.credential?.hash ?: absentCredential)

        return when {
            account == null || !proven -> {
                Result.CredentialNotProven
            }

            !account.maySignIn(policy) -> {
                Result.EmailNotVerified(account.email)
            }

            else -> {
                sessions.start(account.id, clock.instant())
                Result.SignedIn(account.email)
            }
        }
    }

    private fun throttle(
        limit: RateLimit,
        key: String,
    ) = (limit.tryAcquire(key) as? RateLimit.Decision.Denied)?.let { Result.Throttled(it.retryAfter) }

    /** An address too malformed to be one no Account can hold, which is not a distinguishable answer. */
    private fun findAccount(email: String): Account? =
        if (EmailAddress.violations(email).isEmpty()) accounts.findByEmail(EmailAddress(email)) else null
}
