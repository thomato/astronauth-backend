package dev.thomato.auth.account.api.graphql

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.savedrequest.RequestCache
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.net.URI

/**
 * Where the browser was headed when Spring bounced it to the sign-in page. Form login would hand this to a
 * SavedRequestAwareAuthenticationSuccessHandler; a mutation has to ask for it (ADR 0007).
 *
 * The value comes from Spring's own cache rather than from anything the caller sent, so it cannot be used to
 * redirect someone off-site. `/oauth2/authorize` will land in this same cache.
 */
@Component
class SavedRequests(
    private val requestCache: RequestCache,
) {
    /** Reads and forgets the saved request, falling back to the Account page. */
    fun consume(): String {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        val response = attributes?.response
        return if (response == null) DEFAULT else consume(attributes.request, response)
    }

    private fun consume(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): String {
        val saved = requestCache.getRequest(request, response)
        if (saved != null) requestCache.removeRequest(request, response)
        return pathOf(saved?.redirectUrl)
    }

    private fun pathOf(redirectUrl: String?): String {
        val uri = redirectUrl?.let { runCatching { URI(it) }.getOrNull() }
        val path = uri?.rawPath.orEmpty()
        val query =
            uri
                ?.rawQuery
                .orEmpty()
                .split('&')
                .filterNot { it == MATCHING_PARAMETER }
                .joinToString("&")
        return if (path.isBlank()) DEFAULT else path + if (query.isBlank()) "" else "?$query"
    }

    private companion object {
        const val DEFAULT = "/account"

        /**
         * Spring adds this to a saved request's URL so that returning to it replays the original request.
         * We take the request out of the cache here instead, so the marker has no job left and would only
         * show up in the address bar.
         */
        const val MATCHING_PARAMETER = "continue"
    }
}
