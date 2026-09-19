package dev.thomato.auth.account.infrastructure.persistence

import dev.thomato.auth.account.domain.AccountId
import dev.thomato.auth.account.domain.PasswordCredential
import dev.thomato.auth.account.domain.PasswordHash
import dev.thomato.auth.account.domain.VerificationLink
import dev.thomato.auth.account.domain.VerificationLinks
import dev.thomato.auth.account.domain.VerificationTokenHash
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Component
class JdbcVerificationLinks(
    private val jdbc: JdbcClient,
) : VerificationLinks {
    override fun add(link: VerificationLink) {
        jdbc
            .sql(
                """
                INSERT INTO verification_links
                    (token_hash, account_id, password_hash, sent_at, used_at, invalidated_at, wrong_credential_attempts)
                VALUES
                    (:tokenHash, :accountId, :passwordHash, :sentAt, :usedAt, :invalidatedAt, :wrongCredentialAttempts)
                """,
            ).params(parameters(link))
            .update()
    }

    override fun find(tokenHash: VerificationTokenHash) = find(tokenHash, lock = false)

    override fun findForUpdate(tokenHash: VerificationTokenHash) = find(tokenHash, lock = true)

    private fun find(
        tokenHash: VerificationTokenHash,
        lock: Boolean,
    ): VerificationLink? =
        jdbc
            .sql("SELECT * FROM verification_links WHERE token_hash = :tokenHash" + if (lock) " FOR UPDATE" else "")
            .param("tokenHash", tokenHash.value)
            .query(rowMapper)
            .optional()
            .orElse(null)

    override fun update(link: VerificationLink) {
        jdbc
            .sql(
                """
                UPDATE verification_links
                SET used_at = :usedAt, invalidated_at = :invalidatedAt, wrong_credential_attempts = :wrongCredentialAttempts
                WHERE token_hash = :tokenHash
                """,
            ).params(parameters(link))
            .update()
    }

    override fun invalidateOthers(
        accountId: AccountId,
        except: VerificationTokenHash,
        now: Instant,
    ) {
        jdbc
            .sql(
                """
                UPDATE verification_links SET invalidated_at = :now
                WHERE account_id = :accountId AND token_hash <> :except AND used_at IS NULL AND invalidated_at IS NULL
                """,
            ).param("now", Timestamp.from(now))
            .param("accountId", accountId.value)
            .param("except", except.value)
            .update()
    }

    private fun parameters(link: VerificationLink) =
        mapOf(
            "tokenHash" to link.tokenHash.value,
            "accountId" to link.accountId.value,
            "passwordHash" to link.credential.hash.value,
            "sentAt" to Timestamp.from(link.sentAt),
            "usedAt" to link.usedAt?.let(Timestamp::from),
            "invalidatedAt" to link.invalidatedAt?.let(Timestamp::from),
            "wrongCredentialAttempts" to link.wrongCredentialAttempts,
        )

    private val rowMapper =
        RowMapper { rs, _ ->
            VerificationLink(
                tokenHash = VerificationTokenHash(rs.getString("token_hash")),
                accountId = AccountId(rs.getObject("account_id", UUID::class.java)),
                credential = PasswordCredential(PasswordHash(rs.getString("password_hash"))),
                sentAt = rs.getTimestamp("sent_at").toInstant(),
                usedAt = rs.getTimestamp("used_at")?.toInstant(),
                invalidatedAt = rs.getTimestamp("invalidated_at")?.toInstant(),
                wrongCredentialAttempts = rs.getInt("wrong_credential_attempts"),
            )
        }
}
