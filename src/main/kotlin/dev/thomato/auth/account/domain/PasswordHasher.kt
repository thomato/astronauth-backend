package dev.thomato.auth.account.domain

/** Outbound port for turning a Password into a PasswordHash. */
interface PasswordHasher {
    fun hash(password: Password): PasswordHash
}
