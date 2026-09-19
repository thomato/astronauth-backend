package dev.thomato.auth.account.infrastructure.queue

import dev.thomato.auth.account.application.registration.Register
import dev.thomato.auth.account.application.registration.RegistrationRequestQueue
import dev.thomato.auth.account.domain.RegistrationRequest
import org.slf4j.LoggerFactory
import org.springframework.core.task.TaskExecutor

/**
 * Processes Registration requests on another thread, so the request that accepted one never waits for
 * Registration (ADR 0003). In memory: a request is lost if the application crashes before processing it.
 */
class ExecutorRegistrationRequestQueue(
    private val executor: TaskExecutor,
    private val register: Register,
) : RegistrationRequestQueue {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun submit(request: RegistrationRequest) {
        executor.execute {
            @Suppress("TooGenericExceptionCaught") // nothing else would report a failure on this thread
            try {
                register.process(request)
            } catch (e: Exception) {
                log.error("Registration request could not be processed", e)
            }
        }
    }
}
