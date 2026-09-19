package dev.thomato.auth.account

import dev.thomato.auth.TestcontainersConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.HttpGraphQlTester

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureHttpGraphQlTester
@Import(TestcontainersConfiguration::class, RecordingEmailSender.Configuration::class)
class RegistrationTests(
    @Autowired private val graphQlTester: HttpGraphQlTester,
    @Autowired private val emails: RecordingEmailSender,
) {
    @Test
    fun `registering a new email address sends a verification link to that address`() {
        graphQlTester
            .document(
                """
                mutation {
                    requestRegistration(input: { email: "Ada@Example.com", password: "correct horse battery" }) {
                        __typename
                    }
                }
                """,
            ).execute()
            .path("requestRegistration.__typename")
            .entity(String::class.java)
            .isEqualTo("RegistrationRequestAccepted")

        val email = emails.awaitVerificationLink()

        assertThat(email.to.asEntered).isEqualTo("Ada@Example.com")
    }
}
