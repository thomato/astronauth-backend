package dev.thomato.auth.account.domain

/** Outbound port for storing Accounts. */
interface Accounts {
    fun add(account: Account)
}
