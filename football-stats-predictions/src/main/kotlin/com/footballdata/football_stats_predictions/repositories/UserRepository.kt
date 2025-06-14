package com.footballdata.football_stats_predictions.repositories

import com.footballdata.football_stats_predictions.model.Role
import com.footballdata.football_stats_predictions.model.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class UserRepository(
    encoder: PasswordEncoder
) {
    private val users = mutableSetOf(
        User(
            id = UUID.randomUUID(),
            name = "email-1@gmail.com",
            password = encoder.encode("pass1"),
            role = Role.USER,
        ),
        User(
            id = UUID.randomUUID(),
            name = "email-2@gmail.com",
            password = encoder.encode("pass2"),
            role = Role.ADMIN,
        ),
        User(
            id = UUID.randomUUID(),
            name = "email-3@gmail.com",
            password = encoder.encode("pass3"),
            role = Role.USER,
        )
    )

    fun findByUsername(email: String): User? =
        users
            .firstOrNull { it.name == email }

    fun save(user: User) {
        users.add(user)
    }
}