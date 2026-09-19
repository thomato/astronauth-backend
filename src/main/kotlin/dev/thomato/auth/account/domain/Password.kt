package dev.thomato.auth.account.domain

/** A password as typed by the person; it is only ever hashed, never stored. */
class Password(
    val plaintext: String,
) {
    override fun toString() = "Password(****)"
}
