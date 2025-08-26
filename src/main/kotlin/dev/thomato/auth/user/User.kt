package dev.thomato.auth.user

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.*

@Table("users")
data class User(
    @Id
    val id: UUID? = null,
    val email: String,
    val password: String,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)