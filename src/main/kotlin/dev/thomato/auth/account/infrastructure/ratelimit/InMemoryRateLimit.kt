package dev.thomato.auth.account.infrastructure.ratelimit

import dev.thomato.auth.account.application.RateLimit
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Allows at most [max] occurrences per key in any sliding [window]. In memory, like the Registration request
 * queue (ADR 0003): counts reset on restart and are not shared between instances.
 */
class InMemoryRateLimit(
    private val max: Int,
    private val window: Duration,
    private val clock: Clock,
) : RateLimit {
    private val occurrences = ConcurrentHashMap<String, ArrayDeque<Instant>>()
    private val acquisitions = AtomicInteger()

    override fun tryAcquire(key: String): RateLimit.Decision {
        val now = clock.instant()
        var decision: RateLimit.Decision = RateLimit.Decision.Allowed
        occurrences.compute(key) { _, existing ->
            val recent = (existing ?: ArrayDeque()).apply { dropBefore(now - window) }
            if (recent.size < max) {
                recent.addLast(now)
            } else {
                decision = RateLimit.Decision.Denied(Duration.between(now, recent.first() + window))
            }
            recent
        }
        if (acquisitions.incrementAndGet() % FORGET_EVERY == 0) forgetExpired()
        return decision
    }

    /** Forgets keys whose occurrences have all left the window, so memory does not grow with every client seen. */
    private fun forgetExpired() {
        val cutoff = clock.instant() - window
        occurrences.keys.forEach { key ->
            occurrences.computeIfPresent(key) { _, recent -> recent.apply { dropBefore(cutoff) }.ifEmpty { null } }
        }
    }

    private fun ArrayDeque<Instant>.dropBefore(cutoff: Instant) {
        while (isNotEmpty() && !first().isAfter(cutoff)) removeFirst()
    }

    private companion object {
        const val FORGET_EVERY = 1000
    }
}
