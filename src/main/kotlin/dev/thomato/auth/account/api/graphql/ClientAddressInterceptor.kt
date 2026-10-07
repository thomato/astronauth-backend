package dev.thomato.auth.account.api.graphql

import dev.thomato.auth.account.application.ClientAddress
import org.springframework.graphql.server.WebGraphQlInterceptor
import org.springframework.graphql.server.WebGraphQlRequest
import org.springframework.graphql.server.WebGraphQlResponse
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration
import kotlin.math.ceil

/**
 * Makes the client's address available to controllers as `@ContextValue clientAddress`. It is whatever the
 * servlet container trusts: the connection's address, or X-Forwarded-For when forward headers are configured.
 */
@Component
class ClientAddressInterceptor : WebGraphQlInterceptor {
    override fun intercept(
        request: WebGraphQlRequest,
        chain: WebGraphQlInterceptor.Chain,
    ): Mono<WebGraphQlResponse> {
        val address = request.remoteAddress?.address?.hostAddress ?: request.remoteAddress?.hostString ?: UNKNOWN
        request.configureExecutionInput { _, builder ->
            builder.graphQLContext(mapOf(CONTEXT_KEY to ClientAddress(address))).build()
        }
        return chain.next(request)
    }

    companion object {
        const val CONTEXT_KEY = "clientAddress"
        private const val UNKNOWN = "unknown"
    }
}

/** Rounds up, so a client told to wait never retries a moment too early. */
internal fun Duration.toRetryAfterSeconds() = ceil(toMillis() / MILLIS_PER_SECOND).toInt().coerceAtLeast(1)

private const val MILLIS_PER_SECOND = 1000.0
