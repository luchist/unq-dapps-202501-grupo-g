package com.footballdata.football_stats_predictions.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AntPathRequestMatcher


@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    @Profile("!prod") // Aplica esta configuración solo si el perfil NO es 'prod'
    fun developmentSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    // Permite acceso sin autenticación a la consola H2 y a la página de login
                    .requestMatchers(
                        AntPathRequestMatcher("/h2-console/**"),
                        AntPathRequestMatcher("/login") // <-- AÑADIDO: Permite acceso a /login
                    ).permitAll()
                    .anyRequest().authenticated() // Todas las demás peticiones requieren autenticación (ajusta según necesites)
            }
            //.formLogin(Customizer.withDefaults())
            //.httpBasic(Customizer.withDefaults()) // Lo agregué del tutorial
            .csrf { csrf ->
                // Es común necesitar deshabilitar CSRF para la consola H2,
                // ya que a menudo se accede a través de un POST desde su propia UI
                // o si usa frames.
                csrf.ignoringRequestMatchers(AntPathRequestMatcher("/h2-console/**"))
            }
            .headers { headers ->
                // La consola H2 a menudo se muestra en un iframe.
                // Para permitirlo, necesitas deshabilitar X-Frame-Options o configurarlo adecuadamente.
                // 'sameOrigin()' permitiría iframes desde el mismo origen.
                // Para la consola H2, a veces es más simple deshabilitarlo para esa ruta.
                headers.frameOptions { frameOptions -> frameOptions.sameOrigin() } // O .disable() si sameOrigin() no es suficiente
            }

        return http.build()
    }
}