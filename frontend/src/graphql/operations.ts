import { graphql } from './generated';

export const RequestRegistration = graphql(`
  mutation RequestRegistration($input: RequestRegistrationInput!) {
    requestRegistration(input: $input) {
      __typename
      ... on RegistrationRequestAccepted {
        email
      }
      ... on RegistrationRequestRejected {
        violations {
          field
          code
          limit
        }
      }
      ... on RegistrationRequestThrottled {
        retryAfterSeconds
      }
    }
  }
`);

export const LookUpVerificationLink = graphql(`
  query LookUpVerificationLink($token: String!) {
    verificationLink(token: $token) {
      __typename
      ... on VerificationLink {
        status
        email
      }
      ... on VerificationThrottled {
        retryAfterSeconds
      }
    }
  }
`);

export const CompleteEmailVerification = graphql(`
  mutation CompleteEmailVerification($input: CompleteEmailVerificationInput!) {
    completeEmailVerification(input: $input) {
      __typename
      ... on EmailVerified {
        email
      }
      ... on WrongCredential {
        attemptsLeft
      }
      ... on VerificationLinkUnusable {
        status
      }
      ... on VerificationThrottled {
        retryAfterSeconds
      }
    }
  }
`);
