package dev.thomato.auth.account.domain

/**
 * The Operator's rule for what an Account without a Verified email may do; see CONTEXT.md. It is a value the
 * use cases pass in, like the clock, so the rule stays testable without configuration (ADR 0001).
 */
enum class VerificationPolicy {
    /** An Account may sign in before its email address is a Verified email. */
    OPTIONAL,

    /** An Account may sign in only once its email address is a Verified email. */
    REQUIRED,
}
