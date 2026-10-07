package dev.thomato.auth.account.application.signin

import dev.thomato.auth.account.application.Sessions

/**
 * Sign-out: ends the one Session doing the signing out. Ending every Session of an Account is a different
 * thing, reserved for a Credential that is no longer trusted (ADR 0006).
 */
class SignOut(
    private val sessions: Sessions,
) {
    fun end() = sessions.endCurrent()
}
