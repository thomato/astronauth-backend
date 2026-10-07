package dev.thomato.auth.account.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class RegistrationRulesTests {
    @ParameterizedTest
    @ValueSource(
        strings = ["ada@example.com", "  Ada.Lovelace+test@Example.co.uk ", "\"quoted\"@example.com", "jan@münster.de"],
    )
    fun `loose email addresses are accepted`(address: String) {
        assertThat(EmailAddress.violations(address)).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["", "ada", "ada@", "@example.com", "ada@localhost", "ada@example.", "a b@example.com", "a@b@c.com"],
    )
    fun `addresses without the basic shape are invalid`(address: String) {
        assertThat(EmailAddress.violations(address)).containsExactly(Violation.EmailInvalid)
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 65])
    fun `the local part is at most 64 characters`(length: Int) {
        val violations = EmailAddress.violations("a".repeat(length) + "@example.com")
        assertThat(violations.isEmpty()).isEqualTo(length <= 64)
    }

    @ParameterizedTest
    @ValueSource(ints = [254, 255])
    fun `an address is at most 254 characters`(length: Int) {
        val domain = "d".repeat(length - "a@.nl".length)
        val violations = EmailAddress.violations("a@$domain.nl")
        assertThat(violations).isEqualTo(if (length <= 254) emptyList() else listOf(Violation.EmailTooLong(254)))
    }

    @ParameterizedTest
    @ValueSource(ints = [11, 12, 128, 129])
    fun `a password is 12 to 128 characters`(length: Int) {
        val violations = Password("p".repeat(length)).violations
        val expected =
            when {
                length < 12 -> listOf(Violation.PasswordTooShort(12))
                length > 128 -> listOf(Violation.PasswordTooLong(128))
                else -> emptyList()
            }
        assertThat(violations).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(strings = ["🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀", "            "])
    fun `password length counts characters, including emoji and spaces`(password: String) {
        assertThat(Password(password).violations).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = ["café"])
    fun `passwords are normalised so the same characters always match`(decomposed: String) {
        assertThat(Password(decomposed).plaintext).isEqualTo(Password("café").plaintext)
    }
}
