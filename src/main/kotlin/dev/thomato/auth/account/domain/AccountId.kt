package dev.thomato.auth.account.domain

import java.util.UUID

/** The stable identifier of an Account; client applications receive it as the subject of their tokens. */
@JvmInline
value class AccountId(
    val value: UUID,
)
