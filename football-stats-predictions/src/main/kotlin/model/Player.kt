package model

class Player(
    val playerName: String,
    var position: String,
    var shoots: Int,
    var interceptions: Int
) {
    fun calculatePerformanceIndex() {
        throw NotImplementedError()
    }
}