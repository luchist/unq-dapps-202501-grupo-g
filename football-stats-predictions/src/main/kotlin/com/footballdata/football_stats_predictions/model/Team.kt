package com.footballdata.football_stats_predictions.model

class Team(
    val teamName: String,
    var players: List<Player>
) {
    fun getTeamPlayers() {
        throw NotImplementedError()
    }
}