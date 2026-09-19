package dev.thomato.auth.account.domain

/** The secret in a verification link; only its hash is ever stored. */
@JvmInline
value class VerificationToken(
    val value: String,
) {
    override fun toString() = "VerificationToken(****)"
}
