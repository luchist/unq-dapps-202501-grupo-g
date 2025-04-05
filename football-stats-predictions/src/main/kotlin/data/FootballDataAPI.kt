package data

class FootballDataAPI(
    val apiUrl: String
) : StatisticsProvider {
    override fun getTeamStatistics(teamName: String): TeamStatistics {
        TODO("Not yet implemented")
    }

    override fun getPlayerStatistics(playerName: String): PlayerStatistics {
        TODO("Not yet implemented")
    }

    override fun getMatchStatistics(matchId: String): MatchStatistics {
        TODO("Not yet implemented")
    }

    fun getMatchResult(matchDate: String, leagueName: String) {
        TODO("Not yet implemented")
    }

    fun getFixtureLeague(leagueName: String) {
        TODO("Not yet implemented")
    }

    fun getTeamSetup(teamName: String, matchDate: String, leagueName: String) {
        TODO("Not yet implemented")
    }
}