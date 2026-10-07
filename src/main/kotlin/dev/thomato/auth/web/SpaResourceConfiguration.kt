package dev.thomato.auth.web

import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Duration

/** Vite puts a content hash in every asset's name, so a cached asset can never go stale. */
@Configuration(proxyBeanMethods = false)
class SpaResourceConfiguration : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry
            .addResourceHandler("/assets/**")
            .addResourceLocations("classpath:/static/assets/")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(DAYS_IN_A_YEAR)).cachePublic().immutable())
    }

    private companion object {
        const val DAYS_IN_A_YEAR = 365L
    }
}
