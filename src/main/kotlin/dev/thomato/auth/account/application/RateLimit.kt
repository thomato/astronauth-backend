package dev.thomato.auth.account.application

import java.time.Duration

/** Outbound port that limits how often something may happen per key, such as per client or per email address. */
interface RateLimit {
    sealed interface Decision {
        data object Allowed : Decision

        data class Denied(
            val retryAfter: Duration,
        ) : Decision
    }

    /** Counts an occurrence for the key if the limit allows it. */
    fun tryAcquire(key: String): Decision
}
