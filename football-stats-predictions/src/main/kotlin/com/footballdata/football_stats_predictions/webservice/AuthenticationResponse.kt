package com.footballdata.football_stats_predictions.webservice

data class AuthenticationResponse(
    val accessToken: String,
    val refreshToken: String
)