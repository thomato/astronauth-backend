package dev.thomato.auth.account.infrastructure

import dev.thomato.auth.account.application.EmailSender
import dev.thomato.auth.account.application.PasswordHasher
import dev.thomato.auth.account.application.RateLimit
import dev.thomato.auth.account.application.Sessions
import dev.thomato.auth.account.application.UnitOfWork
import dev.thomato.auth.account.application.registration.AcceptRegistrationRequest
import dev.thomato.auth.account.application.registration.Register
import dev.thomato.auth.account.application.registration.RegistrationRequestQueue
import dev.thomato.auth.account.application.verification.CompleteEmailVerification
import dev.thomato.auth.account.application.verification.LookUpVerificationLink
import dev.thomato.auth.account.domain.Accounts
import dev.thomato.auth.account.domain.VerificationLinks
import dev.thomato.auth.account.infrastructure.email.SmtpEmailSender
import dev.thomato.auth.account.infrastructure.queue.ExecutorRegistrationRequestQueue
import dev.thomato.auth.account.infrastructure.ratelimit.InMemoryRateLimit
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.mail.MailSender
import java.time.Clock

/** Wires the framework-free use cases to their adapters. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LimitProperties::class)
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
        verificationLinks: VerificationLinks,
        emailSender: EmailSender,
        limits: LimitProperties,
        clock: Clock,
    ) = Register(accounts, verificationLinks, emailSender, limits.emailsPerAddress.toRateLimit(clock), clock)

    @Bean
    fun registrationRequestQueue(
        applicationTaskExecutor: TaskExecutor,
        register: Register,
    ): RegistrationRequestQueue = ExecutorRegistrationRequestQueue(applicationTaskExecutor, register)

    @Bean
    fun acceptRegistrationRequest(
        passwordHasher: PasswordHasher,
        queue: RegistrationRequestQueue,
        limits: LimitProperties,
        clock: Clock,
    ) = AcceptRegistrationRequest(passwordHasher, queue, limits.registration.toRateLimit(clock))

    /** Shared by both verification use cases, so looking up and attempting count against one limit. */
    @Bean
    fun verificationClientLimit(
        limits: LimitProperties,
        clock: Clock,
    ): RateLimit = limits.verification.toRateLimit(clock)

    @Bean
    fun lookUpVerificationLink(
        verificationLinks: VerificationLinks,
        accounts: Accounts,
        verificationClientLimit: RateLimit,
        clock: Clock,
    ) = LookUpVerificationLink(verificationLinks, accounts, verificationClientLimit, clock)

    @Bean
    @Suppress("LongParameterList") // one per port the use case needs
    fun completeEmailVerification(
        verificationLinks: VerificationLinks,
        accounts: Accounts,
        passwordHasher: PasswordHasher,
        sessions: Sessions,
        unitOfWork: UnitOfWork,
        verificationClientLimit: RateLimit,
        clock: Clock,
    ) = CompleteEmailVerification(
        verificationLinks,
        accounts,
        passwordHasher,
        sessions,
        unitOfWork,
        verificationClientLimit,
        clock,
    )

    private fun LimitProperties.Limit.toRateLimit(clock: Clock) = InMemoryRateLimit(max, window, clock)
}
