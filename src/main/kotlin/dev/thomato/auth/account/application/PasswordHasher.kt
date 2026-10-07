package dev.thomato.auth.account.application

import dev.thomato.auth.account.domain.Password
import dev.thomato.auth.account.domain.PasswordHash

/** Outbound port for turning a Password into a PasswordHash and checking one against it. */
interface PasswordHasher {
    fun hash(password: Password): PasswordHash

    fun matches(
        password: Password,
        hash: PasswordHash,
    ): Boolean
}
