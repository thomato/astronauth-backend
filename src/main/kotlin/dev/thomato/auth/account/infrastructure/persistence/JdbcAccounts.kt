package dev.thomato.auth.account.infrastructure.persistence

import dev.thomato.auth.account.domain.Account
import dev.thomato.auth.account.domain.AccountId
import dev.thomato.auth.account.domain.Accounts
import dev.thomato.auth.account.domain.EmailAddress
import dev.thomato.auth.account.domain.PasswordCredential
import dev.thomato.auth.account.domain.PasswordHash
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.util.UUID

@Component
class JdbcAccounts(
    private val jdbc: JdbcClient,
) : Accounts {
    override fun add(account: Account): Boolean =
        jdbc
            .sql(
                """
                INSERT INTO accounts (id, email, canonical_email, password_hash, email_verified_at, registered_at)
                VALUES (:id, :email, :canonicalEmail, :passwordHash, :emailVerifiedAt, :registeredAt)
                ON CONFLICT (canonical_email) DO NOTHING
                """,
            ).params(parameters(account))
            .update() == 1

    override fun findByEmail(email: EmailAddress): Account? =
        jdbc
            .sql("SELECT * FROM accounts WHERE canonical_email = :canonicalEmail")
            .param("canonicalEmail", email.canonical)
            .query(rowMapper)
            .optional()
            .orElse(null)

    override fun get(id: AccountId): Account =
        jdbc
            .sql("SELECT * FROM accounts WHERE id = :id")
            .param("id", id.value)
            .query(rowMapper)
            .single()

    override fun update(account: Account) {
        jdbc
            .sql(
                """
                UPDATE accounts
                SET email = :email, canonical_email = :canonicalEmail, password_hash = :passwordHash,
                    email_verified_at = :emailVerifiedAt, registered_at = :registeredAt
                WHERE id = :id
                """,
            ).params(parameters(account))
            .update()
    }

    private fun parameters(account: Account) =
        mapOf(
            "id" to account.id.value,
            "email" to account.email.asEntered,
            "canonicalEmail" to account.email.canonical,
            "passwordHash" to account.credential.hash.value,
            "emailVerifiedAt" to account.emailVerifiedAt?.let(Timestamp::from),
            "registeredAt" to Timestamp.from(account.registeredAt),
        )

    private val rowMapper =
        RowMapper { rs, _ ->
            Account(
                id = AccountId(rs.getObject("id", UUID::class.java)),
                email = EmailAddress(rs.getString("email")),
                credential = PasswordCredential(PasswordHash(rs.getString("password_hash"))),
                registeredAt = rs.getTimestamp("registered_at").toInstant(),
                emailVerifiedAt = rs.getTimestamp("email_verified_at")?.toInstant(),
            )
        }
}
