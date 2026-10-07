package dev.thomato.auth.account.api.graphql

import dev.thomato.auth.account.application.ClientAddress
import dev.thomato.auth.account.application.signin.LookUpSignedInAccount
import dev.thomato.auth.account.application.signin.SignIn
import dev.thomato.auth.account.application.signin.SignOut
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.ContextValue
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class SignInController(
    private val signIn: SignIn,
    private val signOut: SignOut,
    private val lookUpSignedInAccount: LookUpSignedInAccount,
    private val savedRequests: SavedRequests,
) {
    data class SignInInput(
        val email: String,
        val password: String,
    )

    data class SignedInAccount(
        val email: String,
        val hasVerifiedEmail: Boolean,
    )

    // Class names match the GraphQL union members so Spring GraphQL can resolve the type
    data class SignedIn(
        val email: String,
        val continueTo: String,
    )

    data class CredentialNotProven(
        val proven: Boolean = false,
    )

    data class EmailNotVerified(
        val email: String,
    )

    data class SignInThrottled(
        val retryAfterSeconds: Int,
    )

    @QueryMapping
    fun me(): SignedInAccount? {
        val account = lookUpSignedInAccount.lookUp() ?: return null
        return SignedInAccount(account.email.asEntered, account.hasVerifiedEmail)
    }

    @MutationMapping(name = "signIn")
    fun signInMutation(
        @Argument input: SignInInput,
        @ContextValue(ClientAddressInterceptor.CONTEXT_KEY) client: ClientAddress,
    ): Any =
        when (val result = signIn.attempt(input.email, input.password, client)) {
            is SignIn.Result.SignedIn -> SignedIn(result.email.asEntered, savedRequests.consume())
            SignIn.Result.CredentialNotProven -> CredentialNotProven()
            is SignIn.Result.EmailNotVerified -> EmailNotVerified(result.email.asEntered)
            is SignIn.Result.Throttled -> SignInThrottled(result.retryAfter.toRetryAfterSeconds())
        }

    @MutationMapping(name = "signOut")
    fun signOutMutation(): Boolean {
        signOut.end()
        return true
    }
}
