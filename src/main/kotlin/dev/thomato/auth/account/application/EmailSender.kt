package dev.thomato.auth.account.application

import dev.thomato.auth.account.domain.EmailAddress
import dev.thomato.auth.account.domain.VerificationToken

/** Outbound port for the emails Astronauth sends; one operation per kind of email. */
interface EmailSender {
    fun sendVerificationLink(
        to: EmailAddress,
        token: VerificationToken,
    )

    /** Tells the owner of an Account with a Verified email that someone tried to register their address. */
    fun sendRegistrationNotice(to: EmailAddress)
}
