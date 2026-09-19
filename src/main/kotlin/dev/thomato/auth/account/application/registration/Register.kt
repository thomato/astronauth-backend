package dev.thomato.auth.account.application.registration

import dev.thomato.auth.account.application.EmailSender
import dev.thomato.auth.account.application.RateLimit
import dev.thomato.auth.account.application.VerificationTokenGenerator
import dev.thomato.auth.account.domain.Account
import dev.thomato.auth.account.domain.AccountId
import dev.thomato.auth.account.domain.Accounts
import dev.thomato.auth.account.domain.RegistrationRequest
import dev.thomato.auth.account.domain.VerificationLink
import dev.thomato.auth.account.domain.VerificationLinks
import java.time.Clock
import java.util.UUID

/**
 * Registration: processes a Registration request after the request that accepted it has returned, so what
 * happens here may depend on whether the email address belongs to an Account (ADR 0003).
 */
class Register(
    private val accounts: Accounts,
    private val verificationLinks: VerificationLinks,
    private val emailSender: EmailSender,
    private val emailLimit: RateLimit,
    private val clock: Clock,
) {
    fun process(request: RegistrationRequest) {
        // Silent, and only here: a visible limit per email address would reveal that it has an Account
        if (emailLimit.tryAcquire(request.email.canonical) is RateLimit.Decision.Denied) return

        val newAccount = Account.register(AccountId(UUID.randomUUID()), request, clock.instant())
        // Two Registration requests for a new address can race; the one that loses finds the other's Account
        val account = if (accounts.add(newAccount)) newAccount else checkNotNull(accounts.findByEmail(request.email))

        if (account.hasVerifiedEmail) {
            emailSender.sendRegistrationNotice(account.email)
        } else {
            sendVerificationLink(account, request)
        }
    }

    /** Bound to this request's Credential, which need not be the Account's; older links stay usable (ADR 0006). */
    private fun sendVerificationLink(
        account: Account,
        request: RegistrationRequest,
    ) {
        val token = VerificationTokenGenerator.generate()
        verificationLinks.add(VerificationLink.send(token, account.id, request.credential, clock.instant()))
        emailSender.sendVerificationLink(account.email, token)
    }
}
