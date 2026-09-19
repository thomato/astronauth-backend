package dev.thomato.auth.account.application.registration

import dev.thomato.auth.account.domain.RegistrationRequest

/** Outbound port that hands Registration requests over to be processed after the request that accepted them. */
interface RegistrationRequestQueue {
    fun submit(request: RegistrationRequest)
}
