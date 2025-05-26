package com.footballdata.football_stats_predictions.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class AuthenticationRequest(
    @field:Schema(description = "Username", example = "john_doe")
    @field:NotBlank(message = "Username cannot be empty")
    val username: String,

    @field:Schema(description = "Password", example = "password123")
    @field:NotBlank(message = "Password cannot be empty")
    val password: String
)