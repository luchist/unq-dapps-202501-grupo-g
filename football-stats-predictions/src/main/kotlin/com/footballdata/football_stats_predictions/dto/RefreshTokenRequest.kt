package com.footballdata.football_stats_predictions.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequest(
    @field:Schema(description = "A valid refresh token", example = "eyJ...")
    @field:NotBlank(message = "Refresh token cannot be empty")
    val token: String
)