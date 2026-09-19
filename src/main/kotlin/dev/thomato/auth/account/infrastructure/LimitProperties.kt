package dev.thomato.auth.account.infrastructure

import org.springframework.boot.context.properties.ConfigurationProperties
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
) {
    data class Limit(
        val max: Int,
        val window: Duration,
    )
}
