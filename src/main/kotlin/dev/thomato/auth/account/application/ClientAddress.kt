package dev.thomato.auth.account.application

/**
 * The network address a request came from, as far as Astronauth can trust it; used only to limit clients.
 * Not a value class: Kotlin would mangle the names of controller methods that take one.
 */
data class ClientAddress(
    val value: String,
)
