package dev.thomato.auth.account.domain

/** Outbound port that hands Registration requests over to be processed after the request that accepted them. */
interface RegistrationRequestQueue {
    fun submit(request: RegistrationRequest)
}
