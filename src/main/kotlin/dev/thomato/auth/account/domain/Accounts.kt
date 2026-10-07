package dev.thomato.auth.account.domain

/** Outbound port for storing Accounts. */
interface Accounts {
    /** Adds the Account unless its email address already belongs to one; returns whether it was added. */
    fun add(account: Account): Boolean

    fun findByEmail(email: EmailAddress): Account?

    fun get(id: AccountId): Account

    fun update(account: Account)
}
