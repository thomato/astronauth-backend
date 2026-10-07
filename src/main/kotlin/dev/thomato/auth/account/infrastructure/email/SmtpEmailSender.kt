package dev.thomato.auth.account.infrastructure.email

import dev.thomato.auth.account.application.EmailSender
import dev.thomato.auth.account.domain.EmailAddress
import dev.thomato.auth.account.domain.VerificationToken
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.web.util.UriComponentsBuilder

class SmtpEmailSender(
    private val mailSender: MailSender,
    private val from: String,
    private val publicUrl: String,
) : EmailSender {
    override fun sendVerificationLink(
        to: EmailAddress,
        token: VerificationToken,
    ) {
        val link =
            UriComponentsBuilder
                .fromUriString(publicUrl)
                .path("/verify-email")
                .queryParam("token", token.value)
                .toUriString()
        val text =
            """
            Open this link to verify your email address:

            $link

            You'll be asked for the password you chose when you registered.
            The link works once and expires in 24 hours.

            If you didn't register, you can ignore this email.
            """.trimIndent()
        send(to, "Verify your email address", text)
    }

    override fun sendRegistrationNotice(to: EmailAddress) {
        val text =
            """
            Someone tried to register with this email address, but it already belongs to an account.

            If it was you, sign in instead. If it wasn't, you can ignore this email: nothing has changed.
            """.trimIndent()
        send(to, "Someone tried to register with your email address", text)
    }

    private fun send(
        to: EmailAddress,
        subject: String,
        text: String,
    ) {
        mailSender.send(
            SimpleMailMessage().apply {
                setFrom(from)
                setTo(to.asEntered)
                this.subject = subject
                this.text = text
            },
        )
    }
}
