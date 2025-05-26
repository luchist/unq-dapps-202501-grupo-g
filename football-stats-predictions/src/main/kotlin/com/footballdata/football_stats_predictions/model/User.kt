package com.footballdata.football_stats_predictions.model

import java.util.*

data class User(
    val id: UUID,
    val name: String,
    val password: String,
    val role: Role
)

enum class Role {
    USER, ADMIN
}