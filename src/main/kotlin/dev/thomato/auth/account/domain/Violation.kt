package dev.thomato.auth.account.domain

/** A rule that an email address or password as entered breaks; found before anything else happens to it. */
sealed interface Violation {
    data object EmailInvalid : Violation

    data class EmailTooLong(
        val limit: Int,
    ) : Violation

    data class PasswordTooShort(
        val limit: Int,
    ) : Violation

    data class PasswordTooLong(
        val limit: Int,
    ) : Violation
}
