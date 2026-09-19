package dev.thomato.auth.account.infrastructure.persistence

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("accounts")
data class AccountRow(
    @Id val id: UUID,
    val email: String,
    val canonicalEmail: String,
    val passwordHash: String,
    val emailVerifiedAt: Instant?,
    val registeredAt: Instant,
)
