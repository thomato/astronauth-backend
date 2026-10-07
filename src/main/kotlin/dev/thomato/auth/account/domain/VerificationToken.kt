package dev.thomato.auth.account.domain

import java.security.MessageDigest
import java.util.HexFormat

/** The secret in a Verification link; only its hash is ever stored. */
@JvmInline
value class VerificationToken(
    val value: String,
) {
    /** A plain digest is enough: the token is random and long, so it cannot be guessed from its hash. */
    fun hash() =
        VerificationTokenHash(
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toByteArray())),
        )

    override fun toString() = "VerificationToken(****)"
}

@JvmInline
value class VerificationTokenHash(
    val value: String,
)
