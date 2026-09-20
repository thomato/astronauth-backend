package dev.thomato.auth.account.infrastructure.security

import dev.thomato.auth.account.application.Sessions
import dev.thomato.auth.account.domain.AccountId
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.Session
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Instant
import java.util.UUID

/**
 * Sessions are Spring Sessions, indexed by principal name, which for a signed-in Account is its AccountId.
 *
 * Because Sign-in is a mutation rather than form login (ADR 0007), none of this is done for us: the session
 * id has to be changed by hand, and the SecurityContext has to be saved by hand. Both are covered by tests.
 */
@Component
class SpringSessions(
    private val repository: FindByIndexNameSessionRepository<out Session>,
) : Sessions {
    private val securityContexts = HttpSessionSecurityContextRepository()
    private val sessionFixationProtection = ChangeSessionIdAuthenticationStrategy()
    private val logout = SecurityContextLogoutHandler()

    override fun start(
        accountId: AccountId,
        authenticatedAt: Instant,
    ) {
        val (request, response) = servlet()
        val authentication = authenticationFor(accountId)

        // A session that existed before the Credential was proven must not survive it
        sessionFixationProtection.onAuthentication(authentication, request, response)

        val context = SecurityContextHolder.createEmptyContext().apply { this.authentication = authentication }
        SecurityContextHolder.setContext(context)
        // Also what puts the principal name index in place, which endAll looks Sessions up by
        securityContexts.saveContext(context, request, response)
        request.session.setAttribute(AUTHENTICATED_AT, authenticatedAt)
    }

    override fun currentAccountId(): AccountId? =
        SecurityContextHolder
            .getContext()
            .authentication
            ?.takeIf { it.isAuthenticated && it !is AnonymousAuthenticationToken }
            ?.name
            ?.let { name -> runCatching { AccountId(UUID.fromString(name)) }.getOrNull() }

    override fun endCurrent() {
        val (request, response) = servlet()
        logout.logout(request, response, SecurityContextHolder.getContext().authentication)
    }

    override fun endAll(accountId: AccountId) {
        repository.findByPrincipalName(accountId.value.toString()).keys.forEach(repository::deleteById)
    }

    private fun authenticationFor(accountId: AccountId): Authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            accountId.value.toString(),
            null,
            AuthorityUtils.NO_AUTHORITIES,
        )

    private fun servlet(): Pair<HttpServletRequest, HttpServletResponse> {
        val attributes = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
        return attributes.request to checkNotNull(attributes.response) { "No response bound to this request" }
    }

    companion object {
        /** When the Session's Credential was proven; OpenID Connect's auth_time will read it. */
        const val AUTHENTICATED_AT = "astronauth.authenticatedAt"
    }
}
