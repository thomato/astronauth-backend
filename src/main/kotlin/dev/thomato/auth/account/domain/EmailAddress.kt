package dev.thomato.auth.account.domain

/** An email address as the person entered it; two addresses are the same when their canonical forms match. */
class EmailAddress(
    asEntered: String,
) {
    val asEntered: String = asEntered.trim()
    val canonical: String = this.asEntered.lowercase()

    override fun equals(other: Any?) = other is EmailAddress && other.canonical == canonical

    override fun hashCode() = canonical.hashCode()

    override fun toString() = asEntered
}
