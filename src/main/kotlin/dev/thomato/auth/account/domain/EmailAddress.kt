package dev.thomato.auth.account.domain

/**
 * An email address as the person entered it; two addresses are the same when their canonical forms match.
 * The rules are deliberately loose: only Email verification proves an address, so strictness would only
 * reject real ones.
 */
class EmailAddress(
    asEntered: String,
) {
    val asEntered: String = asEntered.trim()
    val canonical: String = this.asEntered.lowercase()

    init {
        require(violations(asEntered).isEmpty()) { "Not a valid email address" }
    }

    override fun equals(other: Any?) = other is EmailAddress && other.canonical == canonical

    override fun hashCode() = canonical.hashCode()

    override fun toString() = asEntered

    companion object {
        const val MAX_LENGTH = 254
        private const val MAX_LOCAL_PART_LENGTH = 64

        fun violations(asEntered: String): List<Violation> {
            val trimmed = asEntered.trim()
            return when {
                trimmed.length > MAX_LENGTH -> listOf(Violation.EmailTooLong(MAX_LENGTH))
                !hasValidShape(trimmed) -> listOf(Violation.EmailInvalid)
                else -> emptyList()
            }
        }

        private fun hasValidShape(address: String): Boolean {
            val parts = address.split('@')
            if (parts.size != 2 || address.any(Char::isWhitespace)) return false
            val (localPart, domain) = parts
            val dot = domain.indexOf('.')
            return localPart.length in 1..MAX_LOCAL_PART_LENGTH && dot > 0 && !domain.endsWith('.')
        }
    }
}
