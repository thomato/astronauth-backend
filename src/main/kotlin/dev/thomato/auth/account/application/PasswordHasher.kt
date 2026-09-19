package dev.thomato.auth.account.application

import dev.thomato.auth.account.domain.Password
import dev.thomato.auth.account.domain.PasswordHash

/** Outbound port for turning a Password into a PasswordHash. */
interface PasswordHasher {
    fun hash(password: Password): PasswordHash
}
