package dev.thomato.auth.account.infrastructure

import dev.thomato.auth.account.application.RateLimit
import dev.thomato.auth.account.infrastructure.ratelimit.InMemoryRateLimit
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Clock
import java.time.Duration

/** How often clients and email addresses may do things; see ADR 0003 for why some limits are silent. */
@ConfigurationProperties("astronauth.limits")
data class LimitProperties(
    /** Registration requests per client address; visible to the client. */
    val registration: Limit,
    /** Verification link look-ups and Email verification attempts together, per client address. */
    val verification: Limit,
    /** Emails that Registration sends per email address; silent. */
    val emailsPerAddress: Limit,
    /** Sign-in attempts per client address; visible to the client. */
    val signIn: Limit,
    /**
     * Sign-in attempts per email address, whether or not it has an Account (ADR 0008). Generous on purpose:
     * it is here to slow automation, not someone who has forgotten their password.
     */
    val signInPerAddress: Limit,
) {
    data class Limit(
        val max: Int,
        val window: Duration,
    )
}

internal fun LimitProperties.Limit.toRateLimit(clock: Clock): RateLimit = InMemoryRateLimit(max, window, clock)
