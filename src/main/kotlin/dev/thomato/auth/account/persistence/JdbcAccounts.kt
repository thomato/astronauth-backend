package dev.thomato.auth.account.persistence

import dev.thomato.auth.account.domain.Account
import dev.thomato.auth.account.domain.Accounts
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.stereotype.Component

@Component
class JdbcAccounts(
    private val jdbc: JdbcAggregateOperations,
) : Accounts {
    override fun add(account: Account) {
        jdbc.insert(
            AccountRow(
                id = account.id.value,
                email = account.email.asEntered,
                canonicalEmail = account.email.canonical,
                passwordHash = account.credential.hash.value,
                emailVerifiedAt = null,
                registeredAt = account.registeredAt,
            ),
        )
    }
}
