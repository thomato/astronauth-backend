package dev.thomato.auth

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class WebSecurityConfiguration {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.authorizeHttpRequests {
            it.requestMatchers("/graphiql", "/graphql").permitAll()
                .anyRequest().authenticated()
        }
            .csrf { csrf ->
                csrf.ignoringRequestMatchers("/graphql/**") // Disable CSRF for GraphQL
            }

        return http.build()
    }
}
