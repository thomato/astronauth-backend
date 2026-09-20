package dev.thomato.auth.account.infrastructure

import dev.thomato.auth.account.application.PasswordHasher
import dev.thomato.auth.account.application.Sessions
import dev.thomato.auth.account.application.signin.LookUpSignedInAccount
import dev.thomato.auth.account.application.signin.SignIn
import dev.thomato.auth.account.application.signin.SignOut
import dev.thomato.auth.account.domain.Accounts
import dev.thomato.auth.account.domain.VerificationPolicy
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/** Wires the Sign-in use cases to their adapters; the Verification policy reaches the domain as a value. */
@Configuration(proxyBeanMethods = false)
class SignInConfiguration {
    @Bean
    @Suppress("LongParameterList") // one per port the use case needs
    fun signIn(
        accounts: Accounts,
        passwordHasher: PasswordHasher,
        sessions: Sessions,
        limits: LimitProperties,
        @Value("\${astronauth.verification-policy}") policy: VerificationPolicy,
        clock: Clock,
    ) = SignIn(
        accounts,
        passwordHasher,
        sessions,
        limits.signIn.toRateLimit(clock),
        limits.signInPerAddress.toRateLimit(clock),
        policy,
        clock,
    )

    @Bean
    fun signOut(sessions: Sessions) = SignOut(sessions)

    @Bean
    fun lookUpSignedInAccount(
        sessions: Sessions,
        accounts: Accounts,
    ) = LookUpSignedInAccount(sessions, accounts)
}
