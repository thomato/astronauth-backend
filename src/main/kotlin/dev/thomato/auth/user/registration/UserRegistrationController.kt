package dev.thomato.auth.user.registration

import dev.thomato.auth.user.User
import dev.thomato.auth.user.UserRepository
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller

@Controller
class UserRegistrationController(
    private val userRepository: UserRepository
) {
    @MutationMapping
    suspend fun registerUser(@Argument input: RegisterUserInput): String {
        if (input.password != input.confirmPassword) {
            return "Passwords do not match"
        }
        
        val user = User(
            email = input.email,
            password = input.password
        )
        
        val savedUser = userRepository.save(user)
        return "User registered successfully with ID: ${savedUser.id}"
    }
}
