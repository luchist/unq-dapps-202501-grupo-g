package com.footballdata.football_stats_predictions.model

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

class PlayerBuilder(
    private var playerName: String = "",
    private var position: String = "",
    private var shoots: Int = 0,
    private var interceptions: Int = 0
) {
    fun withName(playerName: String) = apply {
        this.playerName = playerName
    }

    fun withPosition(position: String) = apply {
        this.position = position
    }

    fun withShoots(shoots: Int) = apply {
        this.shoots = shoots
    }

    fun withInterceptions(interceptions: Int) = apply {
        this.interceptions = interceptions
    }

    fun build(): Player {
        val player = Player(
            playerName = playerName,
            position = position,
            shoots = shoots,
            interceptions = interceptions
        )
        return player
    }
}