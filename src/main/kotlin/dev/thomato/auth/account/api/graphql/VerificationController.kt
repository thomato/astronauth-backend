package dev.thomato.auth.account.api.graphql

import dev.thomato.auth.account.application.ClientAddress
import dev.thomato.auth.account.application.verification.CompleteEmailVerification
import dev.thomato.auth.account.application.verification.LookUpVerificationLink
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.ContextValue
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class VerificationController(
    private val lookUpVerificationLink: LookUpVerificationLink,
    private val completeEmailVerification: CompleteEmailVerification,
    private val savedRequests: SavedRequests,
) {
    data class CompleteEmailVerificationInput(
        val token: String,
        val password: String,
    )

    // Class names match the GraphQL union members so Spring GraphQL can resolve the type
    data class VerificationLink(
        val status: String,
        val email: String?,
    )

    data class EmailVerified(
        val email: String,
        val continueTo: String,
    )

    data class WrongCredential(
        val attemptsLeft: Int,
    )

    data class VerificationLinkUnusable(
        val status: String,
    )

    data class VerificationThrottled(
        val retryAfterSeconds: Int,
    )

    @QueryMapping
    fun verificationLink(
        @Argument token: String,
        @ContextValue(ClientAddressInterceptor.CONTEXT_KEY) client: ClientAddress,
    ): Any =
        when (val result = lookUpVerificationLink.lookUp(token, client)) {
            is LookUpVerificationLink.Result.Found -> VerificationLink(result.status.name, result.email.asEntered)
            LookUpVerificationLink.Result.Unknown -> VerificationLink(UNKNOWN, null)
            is LookUpVerificationLink.Result.Throttled -> VerificationThrottled(result.retryAfter.toRetryAfterSeconds())
        }

    @MutationMapping
    fun completeEmailVerification(
        @Argument input: CompleteEmailVerificationInput,
        @ContextValue(ClientAddressInterceptor.CONTEXT_KEY) client: ClientAddress,
    ): Any =
        when (val result = completeEmailVerification.complete(input.token, input.password, client)) {
            is CompleteEmailVerification.Result.Verified -> {
                // Verification signed the person in (ADR 0006), so it answers with a destination too
                EmailVerified(result.email.asEntered, savedRequests.consume())
            }

            is CompleteEmailVerification.Result.WrongCredential -> {
                WrongCredential(result.attemptsLeft)
            }

            is CompleteEmailVerification.Result.Unusable -> {
                VerificationLinkUnusable(result.status?.name ?: UNKNOWN)
            }

            is CompleteEmailVerification.Result.Throttled -> {
                VerificationThrottled(result.retryAfter.toRetryAfterSeconds())
            }
        }

    private companion object {
        const val UNKNOWN = "UNKNOWN"
    }
}
