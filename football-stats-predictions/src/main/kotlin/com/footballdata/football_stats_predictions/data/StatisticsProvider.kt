package com.footballdata.football_stats_predictions.data

interface StatisticsProvider {
    fun getTeamStatistics(teamName: String): TeamStatistics

    fun getPlayerStatistics(playerName: String): PlayerStatistics

    fun getMatchStatistics(matchId: String): MatchStatistics
}

class MatchStatistics {

}

class PlayerStatistics {

}

class TeamStatistics {

}
