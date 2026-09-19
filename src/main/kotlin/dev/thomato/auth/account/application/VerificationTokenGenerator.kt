package dev.thomato.auth.account.application

import dev.thomato.auth.account.domain.VerificationToken
import java.security.SecureRandom
import java.util.Base64

/** Randomness stays out of the domain, like time and identifiers: the domain receives tokens as values. */
object VerificationTokenGenerator {
    private const val SIZE_IN_BYTES = 32
    private val random = SecureRandom()

    fun generate(): VerificationToken {
        val bytes = ByteArray(SIZE_IN_BYTES).also(random::nextBytes)
        return VerificationToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
    }
}
