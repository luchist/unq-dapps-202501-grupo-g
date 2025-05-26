package com.footballdata.football_stats_predictions.dto

data class AuthenticationResponse(
    val accessToken: String,
    val refreshToken: String
)