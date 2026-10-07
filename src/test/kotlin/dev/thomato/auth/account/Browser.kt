package dev.thomato.auth.account

import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

/**
 * WebTestClient forgets cookies between requests, so a test that signs in and then asks who it is would look
 * like two strangers. This keeps them, the way one person's browser does.
 */
class CookieJar : ExchangeFilterFunction {
    private val cookies = ConcurrentHashMap<String, String>()

    operator fun get(name: String): String? = cookies[name]

    override fun filter(
        request: ClientRequest,
        next: ExchangeFunction,
    ): Mono<ClientResponse> {
        val sent =
            ClientRequest
                .from(request)
                .cookies { it.setAll(cookies) }
                .build()
        return next.exchange(sent).doOnNext(::remember)
    }

    private fun remember(response: ClientResponse) {
        response.cookies().forEach { (name, values) ->
            // An emptied cookie is how the server says the Session is gone
            when (val value = values.firstOrNull()?.value) {
                null, "" -> cookies.remove(name)
                else -> cookies[name] = value
            }
        }
    }
}

/** The cookie Spring Session identifies a Session by. */
const val SESSION_COOKIE = "SESSION"

/**
 * One person's browser: its own client address, its own CSRF token and its own cookies, so Sessions from
 * different tests never meet.
 */
fun HttpGraphQlTester.asNewBrowser(
    jar: CookieJar = CookieJar(),
    address: String = uniqueClientAddress(),
): HttpGraphQlTester =
    mutate()
        .header("X-Forwarded-For", address)
        .header("X-XSRF-TOKEN", CSRF_TOKEN)
        .webTestClient { it.defaultCookie("XSRF-TOKEN", CSRF_TOKEN).filter(jar) }
        .build()

/** Both halves of a browser, for tests that need to read its cookies as well as make requests. */
data class Browser(
    val client: HttpGraphQlTester,
    val jar: CookieJar,
) {
    val session get() = jar[SESSION_COOKIE]
}

fun HttpGraphQlTester.newBrowser(address: String = uniqueClientAddress()): Browser {
    val jar = CookieJar()
    return Browser(asNewBrowser(jar, address), jar)
}
