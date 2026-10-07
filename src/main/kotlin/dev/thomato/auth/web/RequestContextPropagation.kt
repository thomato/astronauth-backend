package dev.thomato.auth.web

import io.micrometer.context.ContextRegistry
import io.micrometer.context.ThreadLocalAccessor
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder

/**
 * Spring GraphQL invokes data fetchers off the servlet thread, where RequestContextHolder is empty. Because
 * Sign-in is a mutation (ADR 0007) it has to reach the servlet request and response itself, to change the
 * session id and to let Spring Session write the Session cookie, so we teach context-propagation to carry
 * the request attributes across the hop. Spring GraphQL already registers the same thing for SecurityContext.
 */
@Configuration(proxyBeanMethods = false)
class RequestContextPropagation {
    @PostConstruct
    fun register() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(RequestAttributesAccessor)
    }

    private object RequestAttributesAccessor : ThreadLocalAccessor<RequestAttributes> {
        override fun key(): Any = "dev.thomato.auth.requestAttributes"

        override fun getValue(): RequestAttributes? = RequestContextHolder.getRequestAttributes()

        override fun setValue(value: RequestAttributes) = RequestContextHolder.setRequestAttributes(value)

        override fun setValue() = RequestContextHolder.resetRequestAttributes()
    }
}
