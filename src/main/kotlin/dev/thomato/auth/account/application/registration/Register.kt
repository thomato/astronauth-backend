package dev.thomato.auth.account.application.registration

import dev.thomato.auth.account.application.EmailSender
import dev.thomato.auth.account.application.VerificationTokenGenerator
import dev.thomato.auth.account.domain.Account
import dev.thomato.auth.account.domain.AccountId
import dev.thomato.auth.account.domain.Accounts
import dev.thomato.auth.account.domain.RegistrationRequest
import java.time.Clock
import java.util.UUID

/** Registration: processes a Registration request after the request that accepted it has returned. */
class Register(
    private val accounts: Accounts,
    private val emailSender: EmailSender,
    private val clock: Clock,
) {
    fun process(request: RegistrationRequest) {
        val account = Account.register(AccountId(UUID.randomUUID()), request, clock.instant())
        accounts.add(account)
        emailSender.sendVerificationLink(account.email, VerificationTokenGenerator.generate())
    }
}
