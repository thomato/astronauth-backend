package dev.thomato.auth.account.application

import dev.thomato.auth.account.domain.AccountId

/** Outbound port for the signed-in sessions of Accounts. */
interface Sessions {
    fun endAll(accountId: AccountId)
}
