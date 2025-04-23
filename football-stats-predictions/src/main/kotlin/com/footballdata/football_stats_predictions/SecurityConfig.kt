package com.footballdata.football_stats_predictions

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/v3/api-docs/**",    // Allow access to OpenAPI docs
                    "/swagger-ui/**",     // Allow access to Swagger UI
                    "/swagger-ui.html",
                    "/api/v1/users"
                ).permitAll()
                it.anyRequest().authenticated() // Secure other endpoints
            }
            .csrf { it.disable() }  // Disable CSRF for APIs
        return http.build()
    }
}

