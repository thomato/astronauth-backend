package dev.thomato.auth.account.domain

/** Outbound port for the emails Astronauth sends; one operation per kind of email. */
interface EmailSender {
    fun sendVerificationLink(
        to: EmailAddress,
        token: VerificationToken,
    )
}
