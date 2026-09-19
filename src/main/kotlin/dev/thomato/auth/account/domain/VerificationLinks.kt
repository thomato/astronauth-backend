package dev.thomato.auth.account.domain

import java.time.Instant

/** Outbound port for storing Verification links. */
interface VerificationLinks {
    fun add(link: VerificationLink)

    /** Finds the link and keeps others from changing it until the surrounding unit of work ends. */
    fun findForUpdate(tokenHash: VerificationTokenHash): VerificationLink?

    fun find(tokenHash: VerificationTokenHash): VerificationLink?

    fun update(link: VerificationLink)

    /** Invalidates every usable link of the Account except the given one. */
    fun invalidateOthers(
        accountId: AccountId,
        except: VerificationTokenHash,
        now: Instant,
    )
}
