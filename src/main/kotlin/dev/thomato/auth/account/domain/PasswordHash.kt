package dev.thomato.auth.account.domain

@JvmInline
value class PasswordHash(
    val value: String,
) {
    override fun toString() = "PasswordHash(****)"
}
