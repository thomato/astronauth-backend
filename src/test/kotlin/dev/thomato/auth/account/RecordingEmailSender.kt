package dev.thomato.auth.account

import dev.thomato.auth.account.application.EmailSender
import dev.thomato.auth.account.domain.EmailAddress
import dev.thomato.auth.account.domain.VerificationToken
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Duration
import java.time.Instant

/**
 * Stands in for SMTP, the only unmanaged dependency. Emails are sent off the request thread, so tests await
 * them; tests share one application, so each awaits only emails to its own address.
 */
class RecordingEmailSender : EmailSender {
    sealed interface Email {
        val to: EmailAddress
    }

    data class VerificationLinkEmail(
        override val to: EmailAddress,
        val token: VerificationToken,
    ) : Email

    data class RegistrationNotice(
        override val to: EmailAddress,
    ) : Email

    private val lock = Object()
    private val unread = mutableListOf<Email>()

    override fun sendVerificationLink(
        to: EmailAddress,
        token: VerificationToken,
    ) = record(VerificationLinkEmail(to, token))

    override fun sendRegistrationNotice(to: EmailAddress) = record(RegistrationNotice(to))

    private fun record(email: Email) =
        synchronized(lock) {
            unread += email
            lock.notifyAll()
        }

    /** Returns the next unread email to the address, waiting for it to be sent. */
    fun awaitEmail(
        to: String,
        timeout: Duration = Duration.ofSeconds(5),
    ): Email = checkNotNull(nextEmail(EmailAddress(to), timeout)) { "No email was sent to $to within $timeout" }

    fun awaitVerificationLink(to: String): VerificationLinkEmail =
        awaitEmail(to).let { it as? VerificationLinkEmail ?: error("Expected a verification link but got $it") }

    /** Waits long enough for background Registration to have sent anything it was going to send. */
    fun assertNoEmail(
        to: String,
        wait: Duration = Duration.ofSeconds(1),
    ) {
        val email = nextEmail(EmailAddress(to), wait)
        check(email == null) { "Expected no email to $to but got $email" }
    }

    private fun nextEmail(
        to: EmailAddress,
        timeout: Duration,
    ): Email? {
        val deadline = Instant.now() + timeout
        synchronized(lock) {
            while (true) {
                unread.firstOrNull { it.to == to }?.let {
                    unread.remove(it)
                    return it
                }
                val remaining = Duration.between(Instant.now(), deadline).toMillis()
                if (remaining <= 0) return null
                lock.wait(remaining)
            }
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    class Configuration {
        @Bean
        @Primary
        fun recordingEmailSender() = RecordingEmailSender()
    }
}
