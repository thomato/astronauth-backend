package dev.thomato.auth.account.domain

/** A Credential proven by knowing a password. */
data class PasswordCredential(
    val hash: PasswordHash,
)
