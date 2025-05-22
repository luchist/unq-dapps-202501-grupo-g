package com.footballdata.football_stats_predictions.service

import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service



@Service
class UserDetailsServiceImpl : UserDetailsService {
    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String?): UserDetails {
        val usuario = getById(username)

        return User
            .withUsername(username)
            .password(usuario.password)
            .roles(*usuario.roles!!.toTypedArray<String?>())
            .build()
    }

    @JvmRecord
    data class Usuario(val username: String?, val password: String?, val roles: MutableSet<String?>?)

    companion object {
        fun getById(username: String?): Usuario {
            // "secreto" => [BCrypt] => "$2a$10$56VCAiApLO8NQYeOPiu2De/EBC5RWrTZvLl7uoeC3r7iXinRR1iiq"
            val password = "$2a$10$56VCAiApLO8NQYeOPiu2De/EBC5RWrTZvLl7uoeC3r7iXinRR1iiq"
            val luis = Usuario(
                "luis",
                password,
                mutableSetOf<String?>("USER")
            )

            val ulises = Usuario(
                "ulises",
                password,
                mutableSetOf<String?>("ADMIN")
            )
            val usuarios = listOf<Usuario?>(luis, ulises)

            return usuarios
                .stream()
                .filter { e: Usuario? -> e?.username == username }
                .findFirst()
                .orElse(null)!!
        }
    }
}
