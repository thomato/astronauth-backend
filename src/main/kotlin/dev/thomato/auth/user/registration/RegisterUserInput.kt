package dev.thomato.auth.user.registration

data class RegisterUserInput(
    val email: String,
    val password: String,
    val confirmPassword: String,
)
