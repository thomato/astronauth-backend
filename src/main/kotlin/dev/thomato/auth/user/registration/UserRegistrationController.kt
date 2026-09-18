package dev.thomato.auth.user.registration

import dev.thomato.auth.user.User
import dev.thomato.auth.user.UserRepository
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Controller

@Controller
class UserRegistrationController(
    private val userRepository: UserRepository,
) {
    private val passwordEncoder = BCryptPasswordEncoder()

    @MutationMapping
    fun registerUser(
        @Argument input: RegisterUserInput,
    ): String {
        require(input.password == input.confirmPassword) { "Password and confirm password must match" }

        val hashedPassword = passwordEncoder.encode(input.password)

        val user =
            User(
                email = input.email,
                password = hashedPassword,
            )

        val savedUser = userRepository.save(user)
        return "User registered successfully with ID: ${savedUser.id}"
    }
}
