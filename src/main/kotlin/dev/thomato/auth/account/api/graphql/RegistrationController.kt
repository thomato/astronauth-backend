package dev.thomato.auth.account.api.graphql

import dev.thomato.auth.account.application.ClientAddress
import dev.thomato.auth.account.application.registration.AcceptRegistrationRequest
import dev.thomato.auth.account.domain.Violation
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.ContextValue
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
        val email: String,
    )

    data class RegistrationRequestRejected(
        val violations: List<RegistrationViolation>,
    )

    data class RegistrationViolation(
        val field: String,
        val code: String,
        val limit: Int? = null,
    )

    data class RegistrationRequestThrottled(
        val retryAfterSeconds: Int,
    )

    @MutationMapping
    fun requestRegistration(
        @Argument input: RequestRegistrationInput,
        @ContextValue(ClientAddressInterceptor.CONTEXT_KEY) client: ClientAddress,
    ): Any =
        when (val result = acceptRegistrationRequest.accept(input.email, input.password, client)) {
            is AcceptRegistrationRequest.Result.Accepted -> {
                RegistrationRequestAccepted(result.email.asEntered)
            }

            is AcceptRegistrationRequest.Result.Rejected -> {
                RegistrationRequestRejected(result.violations.map(::toGraphQl))
            }

            is AcceptRegistrationRequest.Result.Throttled -> {
                RegistrationRequestThrottled(result.retryAfter.toRetryAfterSeconds())
            }
        }

    private fun toGraphQl(violation: Violation) =
        when (violation) {
            Violation.EmailInvalid -> RegistrationViolation("EMAIL", "EMAIL_INVALID")
            is Violation.EmailTooLong -> RegistrationViolation("EMAIL", "EMAIL_TOO_LONG", violation.limit)
            is Violation.PasswordTooShort -> RegistrationViolation("PASSWORD", "PASSWORD_TOO_SHORT", violation.limit)
            is Violation.PasswordTooLong -> RegistrationViolation("PASSWORD", "PASSWORD_TOO_LONG", violation.limit)
        }
}
