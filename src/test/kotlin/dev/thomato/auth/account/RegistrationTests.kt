package dev.thomato.auth.account

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.test.tester.HttpGraphQlTester

@AccountIntegrationTest
class RegistrationTests(
    @Autowired private val graphQlTester: HttpGraphQlTester,
    @Autowired private val emails: RecordingEmailSender,
) {
    private val client = graphQlTester.asNewClient()

    @Test
    fun `registering a new email address sends a verification link to that address`() {
        client
            .requestRegistration("  Ada@Example.com ", PASSWORD)
            .path("requestRegistration.__typename")
            .entity(String::class.java)
            .isEqualTo("RegistrationRequestAccepted")
            .path("requestRegistration.email")
            .entity(String::class.java)
            .isEqualTo("Ada@Example.com")

        assertThat(emails.awaitVerificationLink("ada@example.com").to.asEntered).isEqualTo("Ada@Example.com")
    }

    @Test
    fun `every broken rule is reported at once and nothing is sent`() {
        client
            .requestRegistration("not-an-address", "too short")
            .path("requestRegistration.__typename")
            .entity(String::class.java)
            .isEqualTo("RegistrationRequestRejected")
            .path("requestRegistration.violations")
            .entityList(Map::class.java)
            .containsExactly(
                mapOf("field" to "EMAIL", "code" to "EMAIL_INVALID", "limit" to null),
                mapOf("field" to "PASSWORD", "code" to "PASSWORD_TOO_SHORT", "limit" to 12),
            )
    }

    @Test
    fun `passwords longer than the limit are rejected before they are hashed`() {
        client
            .requestRegistration(uniqueEmail(), "x".repeat(129))
            .path("requestRegistration.violations[0].code")
            .entity(String::class.java)
            .isEqualTo("PASSWORD_TOO_LONG")
    }

    @Test
    fun `registering an address that has an unverified Account sends a new link for the new password`() {
        val email = uniqueEmail()
        client.requestRegistration(email, PASSWORD)
        val first = emails.awaitVerificationLink(email)

        client
            .requestRegistration(email, "another password entirely")
            .path("requestRegistration.__typename")
            .entity(String::class.java)
            .isEqualTo("RegistrationRequestAccepted")

        assertThat(emails.awaitVerificationLink(email).token).isNotEqualTo(first.token)
    }

    @Test
    fun `registering an address with a Verified email notifies its owner instead of sending a link`() {
        val email = uniqueEmail()
        client.requestRegistration(email, PASSWORD)
        client.completeEmailVerification(emails.awaitVerificationLink(email).token.value, PASSWORD)

        client.requestRegistration(email, PASSWORD)

        assertThat(emails.awaitEmail(email)).isInstanceOf(RecordingEmailSender.RegistrationNotice::class.java)
    }

    @Test
    fun `at most three emails are sent to an address per hour, without telling the client`() {
        val email = uniqueEmail()
        repeat(3) {
            graphQlTester.asNewClient().requestRegistration(email, PASSWORD)
            emails.awaitVerificationLink(email)
        }

        graphQlTester
            .asNewClient()
            .requestRegistration(email, PASSWORD)
            .path("requestRegistration.__typename")
            .entity(String::class.java)
            .isEqualTo("RegistrationRequestAccepted")

        emails.assertNoEmail(email)
    }

    @Test
    fun `a client that sends too many Registration requests is told to wait`() {
        repeat(10) { client.requestRegistration(uniqueEmail(), PASSWORD) }

        client
            .requestRegistration(uniqueEmail(), PASSWORD)
            .path("requestRegistration.__typename")
            .entity(String::class.java)
            .isEqualTo("RegistrationRequestThrottled")
            .path("requestRegistration.retryAfterSeconds")
            .entity(Int::class.java)
            .satisfies { assertThat(it).isBetween(1, 600) }
    }
}
