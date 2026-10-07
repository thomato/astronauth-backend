package dev.thomato.auth.account.infrastructure.security

import dev.thomato.auth.account.application.Sessions
import dev.thomato.auth.account.domain.AccountId
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.Session
import org.springframework.stereotype.Component

/** Sessions are indexed by principal name, which for a signed-in Account is its AccountId. */
@Component
class SpringSessions(
    private val repository: FindByIndexNameSessionRepository<out Session>,
) : Sessions {
    override fun endAll(accountId: AccountId) {
        repository.findByPrincipalName(accountId.value.toString()).keys.forEach(repository::deleteById)
    }
}
