package dev.thomato.auth.account.application

import dev.thomato.auth.account.domain.EmailAddress
import dev.thomato.auth.account.domain.VerificationToken

/** Outbound port for the emails Astronauth sends; one operation per kind of email. */
interface EmailSender {
    fun sendVerificationLink(
        to: EmailAddress,
        token: VerificationToken,
    )
}
