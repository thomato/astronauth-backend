package dev.thomato.auth.account

import dev.thomato.auth.account.domain.Accounts
import dev.thomato.auth.account.domain.EmailSender
import dev.thomato.auth.account.domain.PasswordHasher
import dev.thomato.auth.account.domain.RegistrationRequestQueue
import dev.thomato.auth.account.email.SmtpEmailSender
import dev.thomato.auth.account.queue.ExecutorRegistrationRequestQueue
import dev.thomato.auth.account.registration.AcceptRegistrationRequest
import dev.thomato.auth.account.registration.Register
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.mail.MailSender
import java.time.Clock

/** Wires the framework-free use cases to their adapters. */
@Configuration(proxyBeanMethods = false)
class AccountConfiguration {
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun emailSender(
        mailSender: MailSender,
        @Value("\${astronauth.mail.from}") from: String,
        @Value("\${astronauth.public-url}") publicUrl: String,
    ): EmailSender = SmtpEmailSender(mailSender, from, publicUrl)

    @Bean
    fun register(
        accounts: Accounts,
        emailSender: EmailSender,
        clock: Clock,
    ) = Register(accounts, emailSender, clock)

    @Bean
    fun registrationRequestQueue(
        applicationTaskExecutor: TaskExecutor,
        register: Register,
    ): RegistrationRequestQueue = ExecutorRegistrationRequestQueue(applicationTaskExecutor, register)

    @Bean
    fun acceptRegistrationRequest(
        passwordHasher: PasswordHasher,
        queue: RegistrationRequestQueue,
    ) = AcceptRegistrationRequest(passwordHasher, queue)
}
