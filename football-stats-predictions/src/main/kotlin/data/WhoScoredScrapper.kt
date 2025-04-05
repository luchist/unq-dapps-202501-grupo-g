package data

class WhoScoredScrapper(
    var urlClient: String
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

    fun getPlayerPerformance(playerName: String) {
        TODO("Not yet implemented")
    }

    fun getTeamPerformance(teamName: String) {
        TODO("Not yet implemented")
    }

    fun scrapData() {
        TODO("Not yet implemented")
    }
}