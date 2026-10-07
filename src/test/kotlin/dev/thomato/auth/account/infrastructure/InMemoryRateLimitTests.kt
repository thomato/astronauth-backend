package dev.thomato.auth.account.infrastructure

import dev.thomato.auth.account.TestClock
import dev.thomato.auth.account.application.RateLimit
import dev.thomato.auth.account.infrastructure.ratelimit.InMemoryRateLimit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class InMemoryRateLimitTests {
    private val clock = TestClock()
    private val limit = InMemoryRateLimit(max = 3, window = Duration.ofHours(1), clock = clock)

    @Test
    fun `allows up to the maximum per key, then says how long to wait`() {
        repeat(3) { assertThat(limit.tryAcquire("a")).isEqualTo(RateLimit.Decision.Allowed) }
        clock.advance(Duration.ofMinutes(20))

        assertThat(limit.tryAcquire("a")).isEqualTo(RateLimit.Decision.Denied(Duration.ofMinutes(40)))
        assertThat(limit.tryAcquire("b")).isEqualTo(RateLimit.Decision.Allowed)
    }

    @Test
    fun `the window slides, so occurrences stop counting an hour after they happened`() {
        limit.tryAcquire("a")
        clock.advance(Duration.ofMinutes(30))
        limit.tryAcquire("a")
        limit.tryAcquire("a")
        clock.advance(Duration.ofMinutes(30))

        assertThat(limit.tryAcquire("a")).isEqualTo(RateLimit.Decision.Allowed)
        assertThat(limit.tryAcquire("a")).isInstanceOf(RateLimit.Decision.Denied::class.java)
    }
}
