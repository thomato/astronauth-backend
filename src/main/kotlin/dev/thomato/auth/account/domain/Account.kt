package dev.thomato.auth.account.domain

import java.time.Instant

class Account private constructor(
    val id: AccountId,
    val email: EmailAddress,
    val credential: PasswordCredential,
    val registeredAt: Instant,
) {
    companion object {
        fun register(
            id: AccountId,
            request: RegistrationRequest,
            now: Instant,
        ) = Account(id, request.email, request.credential, now)
    }
}
