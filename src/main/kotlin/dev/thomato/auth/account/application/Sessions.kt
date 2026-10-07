package dev.thomato.auth.account.application

import dev.thomato.auth.account.domain.AccountId
import java.time.Instant

/** Outbound port for the Sessions of Accounts; see CONTEXT.md. */
interface Sessions {
    /**
     * Starts a Session for the Account in the browser making this request, recording when its Credential was
     * proven. Nothing reads that instant yet; OpenID Connect's `auth_time` and `max_age` will.
     */
    fun start(
        accountId: AccountId,
        authenticatedAt: Instant,
    )

    /** The Account whose Session is making this request, or null when nobody is signed in. */
    fun currentAccountId(): AccountId?

    /** Sign-out: ends the one Session making this request, leaving the Account's other Sessions alone. */
    fun endCurrent()

    /** Ends every Session of the Account, for when the Credential they were opened with is no longer trusted. */
    fun endAll(accountId: AccountId)
}
