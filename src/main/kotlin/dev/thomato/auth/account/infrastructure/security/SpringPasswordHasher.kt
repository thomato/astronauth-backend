package dev.thomato.auth.account.infrastructure.security

import dev.thomato.auth.account.application.PasswordHasher
import dev.thomato.auth.account.domain.Password
import dev.thomato.auth.account.domain.PasswordHash
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.stereotype.Component

/** Hashes with Argon2id; the stored hash carries its algorithm as a prefix, so it can be upgraded later. */
@Component
class SpringPasswordHasher : PasswordHasher {
    private val encoder =
        DelegatingPasswordEncoder(
            "argon2",
            mapOf("argon2" to Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()),
        )

    override fun hash(password: Password) = PasswordHash(checkNotNull(encoder.encode(password.plaintext)))
}
