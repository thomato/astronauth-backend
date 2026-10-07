package dev.thomato.auth.account.infrastructure.persistence

import dev.thomato.auth.account.application.UnitOfWork
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

/** A unit of work is one database transaction. */
@Component
class TransactionalUnitOfWork(
    private val transactions: TransactionTemplate,
) : UnitOfWork {
    // execute() is declared nullable for Java callers; it returns whatever the block returns
    @Suppress("UNCHECKED_CAST")
    override fun <T> run(block: () -> T): T = transactions.execute { block() } as T
}
