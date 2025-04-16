package model

class League(
    var teams: List<Team>,
    var matches: List<Match>
) {
    fun generateConsolidatedReport() {
        throw NotImplementedError()
    }
}