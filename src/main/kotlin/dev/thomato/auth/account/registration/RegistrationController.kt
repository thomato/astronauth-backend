package dev.thomato.auth.account.registration

import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller

@Controller
class RegistrationController(
    private val acceptRegistrationRequest: AcceptRegistrationRequest,
) {
    data class RequestRegistrationInput(
        val email: String,
        val password: String,
    )

    // Class names match the GraphQL union members so Spring GraphQL can resolve the type
    data class RegistrationRequestAccepted(
        val message: String = "Check your inbox to continue.",
    )

    @MutationMapping
    fun requestRegistration(
        @Argument input: RequestRegistrationInput,
    ): Any =
        when (acceptRegistrationRequest.accept(input.email, input.password)) {
            AcceptRegistrationRequest.Result.Accepted -> RegistrationRequestAccepted()
        }
}
