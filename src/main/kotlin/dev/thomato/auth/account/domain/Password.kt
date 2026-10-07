package dev.thomato.auth.account.domain

import java.text.Normalizer

/**
 * A password as typed by the person, normalised so the same characters typed on different devices match;
 * it is only ever hashed, never stored. Length counts characters as people see them, and spaces count.
 */
class Password(
    typed: String,
) {
    val plaintext: String = Normalizer.normalize(typed, Normalizer.Form.NFC)
    private val length = plaintext.codePointCount(0, plaintext.length)

    /** Longer passwords can never have been accepted, so checking them against a hash would only cost work. */
    val isTooLong get() = length > MAX_LENGTH

    val violations: List<Violation>
        get() =
            when {
                length < MIN_LENGTH -> listOf(Violation.PasswordTooShort(MIN_LENGTH))
                isTooLong -> listOf(Violation.PasswordTooLong(MAX_LENGTH))
                else -> emptyList()
            }

    override fun toString() = "Password(****)"

    companion object {
        const val MIN_LENGTH = 12

        /** Bounds the cost of hashing what someone submits. */
        const val MAX_LENGTH = 128
    }
}
