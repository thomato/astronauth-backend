package dev.thomato.auth.account

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * A clock tests can move forward, to see what time does to Verification links and limits.
 * It ticks in microseconds, the precision the database keeps, so an instant read back equals the one stored.
 */
class TestClock(
    @Volatile private var now: Instant = Instant.now().truncatedTo(ChronoUnit.MICROS),
) : Clock() {
    override fun instant(): Instant = now

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    fun advance(duration: Duration) {
        now += duration
    }

    @TestConfiguration(proxyBeanMethods = false)
    class Configuration {
        @Bean
        @Primary
        fun testClock() = TestClock()
    }
}
