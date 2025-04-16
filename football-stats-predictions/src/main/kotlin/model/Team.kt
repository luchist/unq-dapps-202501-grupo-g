package model

class Team(
    val teamName: String,
    var players: List<Player>
) {
    fun getTeamPlayers() {
        throw NotImplementedError()
    }
}