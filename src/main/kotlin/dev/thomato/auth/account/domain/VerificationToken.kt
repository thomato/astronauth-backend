package dev.thomato.auth.account.domain

import java.security.SecureRandom
import java.util.Base64

/** The secret in a verification link; only its hash is ever stored. */
@JvmInline
value class VerificationToken(
    val value: String,
) {
    override fun toString() = "VerificationToken(****)"

    companion object {
        private const val SIZE_IN_BYTES = 32
        private val random = SecureRandom()

        fun generate(): VerificationToken {
            val bytes = ByteArray(SIZE_IN_BYTES).also(random::nextBytes)
            return VerificationToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
        }
    }
}
