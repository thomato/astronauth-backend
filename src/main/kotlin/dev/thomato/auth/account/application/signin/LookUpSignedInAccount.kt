package dev.thomato.auth.account.application.signin

import dev.thomato.auth.account.application.Sessions
import dev.thomato.auth.account.domain.Account
import dev.thomato.auth.account.domain.Accounts

/** The Account behind the Session making this request, for pages that show who is signed in. */
class LookUpSignedInAccount(
    private val sessions: Sessions,
    private val accounts: Accounts,
) {
    fun lookUp(): Account? = sessions.currentAccountId()?.let(accounts::get)
}
