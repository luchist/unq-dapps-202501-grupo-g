package com.footballdata.football_stats_predictions.config

import com.footballdata.football_stats_predictions.repositories.UserRepository
import com.footballdata.football_stats_predictions.service.JwtUserDetailsService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity

class SecurityConfig {

    @Bean
    fun userDetailsService(userRepository: UserRepository): UserDetailsService =
        JwtUserDetailsService(userRepository)

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

    @Bean
    fun authenticationProvider(userRepository: UserRepository): AuthenticationProvider =
        DaoAuthenticationProvider()
            .also {
                it.setUserDetailsService(userDetailsService(userRepository))
                it.setPasswordEncoder(encoder())
            }

    @Bean
    fun filterChain(
        http: HttpSecurity,
        jwtAuthorizationFilter: JwtAuthorizationFilter,
        authenticationProvider: AuthenticationProvider
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configure(http) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/h2-console/**",
                        "/api/auth/**",
                        "/actuator/**"
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .headers { headers ->
                headers.frameOptions { frameOptions -> frameOptions.sameOrigin() }
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, ex ->
                    // 401 for authentication failures
                    response.status = HttpStatus.UNAUTHORIZED.value()
                    response.contentType = "application/json"
                    response.writer.write("{\"error\":\"${ex.message}\"}")
                }
                it.accessDeniedHandler { _, response, ex ->
                    // 403 for authorization failures
                    response.status = HttpStatus.FORBIDDEN.value()
                    response.contentType = "application/json"
                    response.writer.write("{\"error\":\"${ex.message}\"}")
                }
            }

        return http.build()
    }

    @Bean
    fun encoder(): PasswordEncoder = BCryptPasswordEncoder()
}