package dev.thomato.auth

import dev.thomato.auth.web.SpaController
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy
import org.springframework.security.web.savedrequest.HttpSessionRequestCache
import org.springframework.security.web.savedrequest.RequestCache

@Configuration
@EnableWebSecurity
class WebSecurityConfiguration {
    /** One cache, shared by the filter chain that saves a bounced request and the mutation that spends it. */
    @Bean
    fun requestCache(): RequestCache = HttpSessionRequestCache()

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        requestCache: RequestCache,
        @Value("\${spring.graphql.graphiql.enabled:false}") graphiqlEnabled: Boolean,
    ): SecurityFilterChain {
        http
            .authorizeHttpRequests {
                it
                    .requestMatchers(HttpMethod.GET, SpaController.ACCOUNT)
                    .authenticated()
                    .requestMatchers(
                        HttpMethod.GET,
                        SpaController.REGISTER,
                        SpaController.VERIFY_EMAIL,
                        SpaController.SIGN_IN,
                    ).permitAll()
                    .requestMatchers(HttpMethod.GET, "/index.html", "/assets/**", "/favicon.svg")
                    .permitAll()
                    // Permitted so anonymous callers reach signIn and get null from me, rather than a 401
                    .requestMatchers("/graphql", "/error")
                    .permitAll()
                if (graphiqlEnabled) it.requestMatchers("/graphiql").permitAll()
                it.anyRequest().authenticated()
            }
            // Bounces an anonymous request for a protected page to the sign-in page and saves where it was
            // headed; the signIn mutation reads that back, because no success handler runs for it (ADR 0007)
            .exceptionHandling { it.authenticationEntryPoint(LoginUrlAuthenticationEntryPoint(SpaController.SIGN_IN)) }
            .requestCache { it.requestCache(requestCache) }
            // The SPA shares the session's origin (ADR 0004): it reads the XSRF-TOKEN cookie and sends it back
            // as the X-XSRF-TOKEN header on every GraphQL request
            .csrf { it.spa() }
            // Verification links carry their token in the URL; never pass it on to another site
            .headers { it.referrerPolicy { policy -> policy.policy(ReferrerPolicy.NO_REFERRER) } }

        return http.build()
    }
}
