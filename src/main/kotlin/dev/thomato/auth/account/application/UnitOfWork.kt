package dev.thomato.auth.account.application

/** Outbound port that makes everything done inside the block succeed or fail together. */
interface UnitOfWork {
    fun <T> run(block: () -> T): T
}
