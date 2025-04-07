package model

class Match(
    val league: String,
    val date: String,
    var homeTeam: Team,
    var awayTeam: Team
) {
    fun generateMatchPrediction() {
        throw NotImplementedError()
    }
}