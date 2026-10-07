package dev.thomato.auth

import dev.thomato.auth.web.SpaController
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy

@Configuration
@EnableWebSecurity
class WebSecurityConfiguration {
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        @Value("\${spring.graphql.graphiql.enabled:false}") graphiqlEnabled: Boolean,
    ): SecurityFilterChain {
        http
            .authorizeHttpRequests {
                it
                    .requestMatchers(HttpMethod.GET, SpaController.REGISTER, SpaController.VERIFY_EMAIL)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/index.html", "/assets/**", "/favicon.svg")
                    .permitAll()
                    .requestMatchers("/graphql", "/error")
                    .permitAll()
                if (graphiqlEnabled) it.requestMatchers("/graphiql").permitAll()
                it.anyRequest().authenticated()
            }
            // The SPA shares the session's origin (ADR 0004): it reads the XSRF-TOKEN cookie and sends it back
            // as the X-XSRF-TOKEN header on every GraphQL request
            .csrf { it.spa() }
            // Verification links carry their token in the URL; never pass it on to another site
            .headers { it.referrerPolicy { policy -> policy.policy(ReferrerPolicy.NO_REFERRER) } }

        return http.build()
    }
}
