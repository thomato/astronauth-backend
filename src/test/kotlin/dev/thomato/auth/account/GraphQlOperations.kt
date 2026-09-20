package dev.thomato.auth.account

import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.HttpGraphQlTester

fun HttpGraphQlTester.requestRegistration(
    email: String,
    password: String,
): GraphQlTester.Response =
    document(
        """
        mutation(${'$'}input: RequestRegistrationInput!) {
            requestRegistration(input: ${'$'}input) {
                __typename
                ... on RegistrationRequestAccepted { email }
                ... on RegistrationRequestRejected { violations { field code limit } }
                ... on RegistrationRequestThrottled { retryAfterSeconds }
            }
        }
        """,
    ).variable("input", mapOf("email" to email, "password" to password))
        .execute()

fun HttpGraphQlTester.lookUpVerificationLink(token: String): GraphQlTester.Response =
    document(
        """
        query(${'$'}token: String!) {
            verificationLink(token: ${'$'}token) {
                __typename
                ... on VerificationLink { status email }
                ... on VerificationThrottled { retryAfterSeconds }
            }
        }
        """,
    ).variable("token", token)
        .execute()

fun HttpGraphQlTester.completeEmailVerification(
    token: String,
    password: String,
): GraphQlTester.Response =
    document(
        """
        mutation(${'$'}input: CompleteEmailVerificationInput!) {
            completeEmailVerification(input: ${'$'}input) {
                __typename
                ... on EmailVerified { email continueTo }
                ... on WrongCredential { attemptsLeft }
                ... on VerificationLinkUnusable { status }
                ... on VerificationThrottled { retryAfterSeconds }
            }
        }
        """,
    ).variable("input", mapOf("token" to token, "password" to password))
        .execute()

fun HttpGraphQlTester.signIn(
    email: String,
    password: String,
): GraphQlTester.Response =
    document(
        """
        mutation(${'$'}input: SignInInput!) {
            signIn(input: ${'$'}input) {
                __typename
                ... on SignedIn { email continueTo }
                ... on CredentialNotProven { proven }
                ... on EmailNotVerified { email }
                ... on SignInThrottled { retryAfterSeconds }
            }
        }
        """,
    ).variable("input", mapOf("email" to email, "password" to password))
        .execute()

fun HttpGraphQlTester.signOut(): GraphQlTester.Response = document("mutation { signOut }").execute()

fun HttpGraphQlTester.me(): GraphQlTester.Response = document("query { me { email hasVerifiedEmail } }").execute()
