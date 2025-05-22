package com.footballdata.football_stats_predictions.config

import com.footballdata.football_stats_predictions.service.UserDetailsServiceImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
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

// Puedes tener otro SecurityFilterChain para tu configuración de seguridad principal
// que no incluya las reglas específicas de h2-console, o integrarlas aquí.
// Por ejemplo, si quieres que el resto de tu app tenga otra configuración:
/*
@Bean
@Profile("prod") // Configuración para producción
fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
    http
        .authorizeHttpRequests { authorize ->
            authorize
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
        }
        .formLogin { } // O .httpBasic { } o tu configuración de JWT, etc.
        .csrf { csrf -> csrf.disable() } // Considera habilitar CSRF en producción con la configuración adecuada
    return http.build()
}
 */
 */


    /* (1) De forma predeterminada, el formulario de seguridad de Spring Sugin/HTTP Basic Auth está habilitado.
           Sin embargo, tan pronto como se proporciona cualquier configuración basada en servlet,
           El inicio de sesión basado en formularios/y HTTP Basic Auth debe proporcionarse explícitamente.

  * (2) Si nuestra API sin estado usa autenticación basada en token, como JWT,
        No necesitamos protección CSRF
  */
    // Autenticacion con UserDetailsService

    @Bean
    fun userDetailsServiceImpl(): UserDetailsService {
        return UserDetailsServiceImpl()
    }
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }


    /* Autenticacion en Memoria
  @Bean
  public UserDetailsService users() {
    UserDetails user = User.builder()
        .username("user")
        .password("{bcrypt}$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW")
        .roles("USER")
        .build();
    UserDetails admin = User.builder()
        .username("admin")
        .password("{bcrypt}$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW")
        .roles("USER", "ADMIN")
        .build();
    return new InMemoryUserDetailsManager(user, admin);
  }
  */
    @Bean
    @Throws(Exception::class)
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager? {
        return authenticationConfiguration.authenticationManager
    }

}