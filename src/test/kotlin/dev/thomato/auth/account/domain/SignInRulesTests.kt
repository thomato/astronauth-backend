package dev.thomato.auth.account.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class SignInRulesTests {
    private val registeredAt = Instant.parse("2026-09-19T10:00:00Z")

    private val unverified =
        Account(
            AccountId(UUID.randomUUID()),
            EmailAddress("ada@example.com"),
            PasswordCredential(PasswordHash("hash")),
            registeredAt,
        )

    private val verified = unverified.verifyEmail(unverified.credential, registeredAt)

    @Test
    fun `an Account with a Verified email may sign in whatever the policy is`() {
        assertThat(verified.maySignIn(VerificationPolicy.REQUIRED)).isTrue()
        assertThat(verified.maySignIn(VerificationPolicy.OPTIONAL)).isTrue()
    }

    @Test
    fun `an Account without a Verified email may sign in only where the policy is OPTIONAL`() {
        assertThat(unverified.maySignIn(VerificationPolicy.OPTIONAL)).isTrue()
        assertThat(unverified.maySignIn(VerificationPolicy.REQUIRED)).isFalse()
    }

    @Test
    fun `the policy is read at Sign-in, so changing it moves every Account at once`() {
        // The Account is untouched between the two answers: nothing about it records a policy
        assertThat(unverified.maySignIn(VerificationPolicy.REQUIRED)).isFalse()
        assertThat(unverified.maySignIn(VerificationPolicy.OPTIONAL)).isTrue()
        assertThat(unverified.emailVerifiedAt).isNull()
    }
}
