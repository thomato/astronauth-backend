package dev.thomato.auth.account

import dev.thomato.auth.account.application.EmailSender
import dev.thomato.auth.account.domain.EmailAddress
import dev.thomato.auth.account.domain.VerificationToken
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Stands in for SMTP, the only unmanaged dependency; emails are sent off the request thread, so tests await them. */
class RecordingEmailSender : EmailSender {
    data class VerificationLinkEmail(
        val to: EmailAddress,
        val token: VerificationToken,
    )

    private val verificationLinks = LinkedBlockingQueue<VerificationLinkEmail>()

    override fun sendVerificationLink(
        to: EmailAddress,
        token: VerificationToken,
    ) {
        verificationLinks.put(VerificationLinkEmail(to, token))
    }

    fun awaitVerificationLink(timeout: Duration = Duration.ofSeconds(5)): VerificationLinkEmail =
        checkNotNull(verificationLinks.poll(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            "No verification link was sent within $timeout"
        }

    @TestConfiguration(proxyBeanMethods = false)
    class Configuration {
        @Bean
        @Primary
        fun recordingEmailSender() = RecordingEmailSender()
    }
}
