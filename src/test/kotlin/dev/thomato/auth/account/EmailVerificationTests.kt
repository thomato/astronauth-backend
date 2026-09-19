package dev.thomato.auth.account

import dev.thomato.auth.account.application.PasswordHasher
import dev.thomato.auth.account.domain.Accounts
import dev.thomato.auth.account.domain.EmailAddress
import dev.thomato.auth.account.domain.Password
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.HttpGraphQlTester
import java.time.Duration
import java.util.UUID

@AccountIntegrationTest
class EmailVerificationTests(
    @Autowired private val graphQlTester: HttpGraphQlTester,
    @Autowired private val emails: RecordingEmailSender,
    @Autowired private val accounts: Accounts,
    @Autowired private val passwordHasher: PasswordHasher,
    @Autowired private val clock: TestClock,
) {
    private val client = graphQlTester.asNewClient()

    private fun register(
        email: String,
        password: String = PASSWORD,
    ): String {
        graphQlTester.asNewClient().requestRegistration(email, password)
        return emails.awaitVerificationLink(email).token.value
    }

    @Test
    fun `a new Verification link is usable and names the address it was sent to`() {
        val email = "Grace-${UUID.randomUUID()}@Example.com"
        val token = register(email)

        client
            .lookUpVerificationLink(token)
            .path("verificationLink.status")
            .entity(String::class.java)
            .isEqualTo("USABLE")
            .path("verificationLink.email")
            .entity(String::class.java)
            .isEqualTo(email)
    }

    @Test
    fun `an unknown token names no address`() {
        client
            .lookUpVerificationLink("no-such-token")
            .path("verificationLink.status")
            .entity(String::class.java)
            .isEqualTo("UNKNOWN")
            .path("verificationLink.email")
            .valueIsNull()
    }

    @Test
    fun `proving the password verifies the email address and uses up the link`() {
        val email = uniqueEmail()
        val token = register(email)

        client
            .completeEmailVerification(token, PASSWORD)
            .typename("completeEmailVerification")
            .isEqualTo("EmailVerified")

        assertThat(accounts.findByEmail(EmailAddress(email))!!.hasVerifiedEmail).isTrue()
        client
            .lookUpVerificationLink(token)
            .path("verificationLink.status")
            .entity(String::class.java)
            .isEqualTo("USED")
        client
            .completeEmailVerification(token, PASSWORD)
            .path("completeEmailVerification.status")
            .entity(String::class.java)
            .isEqualTo("USED")
    }

    @Test
    fun `a wrong password counts against the link until it is exhausted`() {
        val token = register(uniqueEmail())

        client
            .completeEmailVerification(token, "not the password")
            .path("completeEmailVerification.attemptsLeft")
            .entity(Int::class.java)
            .isEqualTo(4)
        repeat(3) { client.completeEmailVerification(token, "not the password") }

        client
            .completeEmailVerification(token, "not the password")
            .path("completeEmailVerification.status")
            .entity(String::class.java)
            .isEqualTo("EXHAUSTED")
        client
            .completeEmailVerification(token, PASSWORD)
            .path("completeEmailVerification.status")
            .entity(String::class.java)
            .isEqualTo("EXHAUSTED")
    }

    @Test
    fun `a link expires after 24 hours`() {
        val token = register(uniqueEmail())

        clock.advance(Duration.ofHours(24))

        client
            .lookUpVerificationLink(token)
            .path("verificationLink.status")
            .entity(String::class.java)
            .isEqualTo("EXPIRED")
    }

    /** The pre-hijack attack from ADR 0006. */
    @Test
    fun `the owner who verifies with their own password takes the Account over from whoever registered it first`() {
        val email = uniqueEmail()
        val attackersLink = register(email, "the attacker's password")
        val ownersLink = register(email, "the owner's own password")

        client
            .completeEmailVerification(ownersLink, "the attacker's password")
            .typename("completeEmailVerification")
            .isEqualTo("WrongCredential")
        client
            .completeEmailVerification(ownersLink, "the owner's own password")
            .typename("completeEmailVerification")
            .isEqualTo("EmailVerified")

        val credential = accounts.findByEmail(EmailAddress(email))!!.credential
        assertThat(passwordHasher.matches(Password("the owner's own password"), credential.hash)).isTrue()
        assertThat(passwordHasher.matches(Password("the attacker's password"), credential.hash)).isFalse()
        client
            .lookUpVerificationLink(attackersLink)
            .path("verificationLink.status")
            .entity(String::class.java)
            .isEqualTo("INVALIDATED")
    }

    @Test
    fun `a client that looks up or attempts too often is told to wait`() {
        repeat(20) { client.lookUpVerificationLink("no-such-token") }

        client.lookUpVerificationLink("no-such-token").typename("verificationLink").isEqualTo("VerificationThrottled")
        client
            .completeEmailVerification("no-such-token", PASSWORD)
            .typename("completeEmailVerification")
            .isEqualTo("VerificationThrottled")
    }

    private fun GraphQlTester.Response.typename(field: String) = path("$field.__typename").entity(String::class.java)
}
