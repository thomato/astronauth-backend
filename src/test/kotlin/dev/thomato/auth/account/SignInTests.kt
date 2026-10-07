package dev.thomato.auth.account

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.test.tester.HttpGraphQlTester

@AccountIntegrationTest
class SignInTests(
    @Autowired private val graphQlTester: HttpGraphQlTester,
    @Autowired private val emails: RecordingEmailSender,
) {
    /** Registers and verifies, so the Account may sign in under the REQUIRED Verification policy. */
    private fun verifiedAccount(password: String = PASSWORD): String {
        val email = uniqueEmail()
        graphQlTester.asNewClient().requestRegistration(email, password)
        val token = emails.awaitVerificationLink(email).token.value
        graphQlTester.asNewBrowser().completeEmailVerification(token, password)
        return email
    }

    private fun typename(response: org.springframework.graphql.test.tester.GraphQlTester.Response) =
        response.path("signIn.__typename").entity(String::class.java).get()

    @Test
    fun `proving the Credential signs the person in and the Account page knows who they are`() {
        val email = verifiedAccount()
        val browser = graphQlTester.newBrowser()

        browser.client
            .signIn(email, PASSWORD)
            .path("signIn.__typename")
            .entity(String::class.java)
            .isEqualTo("SignedIn")
            .path("signIn.email")
            .entity(String::class.java)
            .isEqualTo(email)
            .path("signIn.continueTo")
            .entity(String::class.java)
            .isEqualTo("/account")

        browser.client
            .me()
            .path("me.email")
            .entity(String::class.java)
            .isEqualTo(email)
            .path("me.hasVerifiedEmail")
            .entity(Boolean::class.java)
            .isEqualTo(true)
    }

    @Test
    fun `nobody signed in is not an error, it is simply nobody`() {
        graphQlTester
            .asNewBrowser()
            .me()
            .path("me")
            .valueIsNull()
    }

    @Test
    fun `a wrong password and an address with no Account are the same answer (ADR 0003)`() {
        val email = verifiedAccount()

        assertThat(typename(graphQlTester.asNewBrowser().signIn(email, "not the password")))
            .isEqualTo("CredentialNotProven")
        assertThat(typename(graphQlTester.asNewBrowser().signIn(uniqueEmail(), PASSWORD)))
            .isEqualTo("CredentialNotProven")
        assertThat(typename(graphQlTester.asNewBrowser().signIn("not-an-address", PASSWORD)))
            .isEqualTo("CredentialNotProven")
    }

    @Test
    fun `a failed Sign-in leaves nobody signed in`() {
        val email = verifiedAccount()
        val browser = graphQlTester.newBrowser()

        browser.client.signIn(email, "not the password")

        browser.client
            .me()
            .path("me")
            .valueIsNull()
    }

    @Test
    fun `an unverified Account is refused under REQUIRED and told which address to verify`() {
        val email = uniqueEmail()
        graphQlTester.asNewClient().requestRegistration(email, PASSWORD)
        emails.awaitVerificationLink(email)
        val browser = graphQlTester.newBrowser()

        browser.client
            .signIn(email, PASSWORD)
            .path("signIn.__typename")
            .entity(String::class.java)
            .isEqualTo("EmailNotVerified")
            .path("signIn.email")
            .entity(String::class.java)
            .isEqualTo(email)

        browser.client
            .me()
            .path("me")
            .valueIsNull()
    }

    @Test
    fun `Sign-in changes the session id, so a session held before it cannot be reused`() {
        val email = verifiedAccount()
        val browser = graphQlTester.newBrowser()

        // Any request that makes the server keep state gives the browser a Session cookie to fixate on
        browser.client.me()
        val before = browser.session

        browser.client.signIn(email, PASSWORD)

        assertThat(browser.session).isNotNull().isNotEqualTo(before)
    }

    @Test
    fun `Sign-out ends that Session and leaves the Account's other Sessions alone`() {
        val email = verifiedAccount()
        val laptop = graphQlTester.newBrowser()
        val phone = graphQlTester.newBrowser()
        laptop.client.signIn(email, PASSWORD)
        phone.client.signIn(email, PASSWORD)

        laptop.client
            .signOut()
            .path("signOut")
            .entity(Boolean::class.java)
            .isEqualTo(true)

        laptop.client
            .me()
            .path("me")
            .valueIsNull()
        phone.client
            .me()
            .path("me.email")
            .entity(String::class.java)
            .isEqualTo(email)
    }

    @Test
    fun `completing Email verification signs the person in (ADR 0006)`() {
        val email = uniqueEmail()
        graphQlTester.asNewClient().requestRegistration(email, PASSWORD)
        val token = emails.awaitVerificationLink(email).token.value
        val browser = graphQlTester.newBrowser()

        browser.client
            .completeEmailVerification(token, PASSWORD)
            .path("completeEmailVerification.__typename")
            .entity(String::class.java)
            .isEqualTo("EmailVerified")
            .path("completeEmailVerification.continueTo")
            .entity(String::class.java)
            .isEqualTo("/account")

        browser.client
            .me()
            .path("me.email")
            .entity(String::class.java)
            .isEqualTo(email)
    }
}
