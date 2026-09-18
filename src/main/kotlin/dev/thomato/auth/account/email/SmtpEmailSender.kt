package dev.thomato.auth.account.email

import dev.thomato.auth.account.domain.EmailAddress
import dev.thomato.auth.account.domain.EmailSender
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
        mailSender.send(
            SimpleMailMessage().apply {
                setFrom(from)
                setTo(to.asEntered)
                subject = "Verify your email address"
                text =
                    """
                    Open this link to verify your email address:

                    $link

                    If you didn't register, you can ignore this email.
                    """.trimIndent()
            },
        )
    }
}
