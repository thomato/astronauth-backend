package dev.thomato.auth.user.registration

import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller

@Controller
class UserRegistrationController {
    @MutationMapping
    fun registerUser(@Argument input: RegisterUserInput): String {
        return "successish"
    }
}
