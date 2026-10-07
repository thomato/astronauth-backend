package dev.thomato.auth.account.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID

class VerificationLinkTests {
    private val sentAt = Instant.parse("2026-09-19T10:00:00Z")
    private val link =
        VerificationLink.send(
            VerificationToken("token"),
            AccountId(UUID.randomUUID()),
            PasswordCredential(PasswordHash("hash")),
            sentAt,
        )

    @Test
    fun `a link is usable until 24 hours after it was sent`() {
        assertThat(link.status(sentAt + Duration.ofHours(24) - Duration.ofSeconds(1)))
            .isEqualTo(VerificationLink.Status.USABLE)
        assertThat(link.status(sentAt + Duration.ofHours(24))).isEqualTo(VerificationLink.Status.EXPIRED)
    }

    @Test
    fun `the fifth wrong password exhausts a link`() {
        val fourWrong = (1..4).fold(link) { attempted, _ -> attempted.recordWrongCredential() }
        assertThat(fourWrong.status(sentAt)).isEqualTo(VerificationLink.Status.USABLE)
        assertThat(fourWrong.recordWrongCredential().status(sentAt)).isEqualTo(VerificationLink.Status.EXHAUSTED)
    }

    @Test
    fun `only the hash of the token is kept`() {
        assertThat(link.tokenHash).isEqualTo(VerificationToken("token").hash())
        assertThat(link.tokenHash.value).doesNotContain("token").hasSize(64)
    }
}
