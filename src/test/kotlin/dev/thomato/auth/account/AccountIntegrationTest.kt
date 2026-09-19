package dev.thomato.auth.account

import dev.thomato.auth.TestcontainersConfiguration
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.web.filter.ForwardedHeaderFilter
import java.util.UUID

/**
 * Runs the whole application over HTTP with the production limits. Tests share it, so each test acts as its own
 * client (by address) and registers its own email addresses, and never sees another test's counts or emails.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureHttpGraphQlTester
@Import(
    TestcontainersConfiguration::class,
    RecordingEmailSender.Configuration::class,
    TestClock.Configuration::class,
    AccountIntegrationTest.Configuration::class,
)
annotation class AccountIntegrationTest {
    @TestConfiguration(proxyBeanMethods = false)
    class Configuration {
        /** Lets a test choose its client address with X-Forwarded-For, as a trusted proxy would. */
        @Bean
        fun forwardedHeaderFilter() = ForwardedHeaderFilter()
    }
}

/** The double-submit value the SPA copies from the XSRF-TOKEN cookie into the X-XSRF-TOKEN header. */
const val CSRF_TOKEN = "test-csrf-token"

/** A client with its own address and a valid CSRF token, like the SPA in one person's browser. */
fun HttpGraphQlTester.asNewClient(address: String = uniqueClientAddress()): HttpGraphQlTester =
    mutate()
        .header("X-Forwarded-For", address)
        .header("X-XSRF-TOKEN", CSRF_TOKEN)
        .webTestClient { it.defaultCookie("XSRF-TOKEN", CSRF_TOKEN) }
        .build()

fun uniqueClientAddress() = "10.${(0..255).random()}.${(0..255).random()}.${(1..254).random()}"

fun uniqueEmail() = "person-${UUID.randomUUID()}@example.com"

const val PASSWORD = "correct horse battery"
