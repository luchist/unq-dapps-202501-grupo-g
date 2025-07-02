package com.footballdata.football_stats_predictions.model

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
class Player(
    @Id
    val id: Long,
    val playerName: String,
    var position: String,
    val dateOfBirth: String,
    val nationality: String,
    var shoots: Int,
    var interceptions: Int
)

class PlayerBuilder(
    private var id: Long = 0,
    private var playerName: String = "",
    private var position: String = "",
    private var dateOfBirth: String = "",
    private var nationality: String = "",
    private var shoots: Int = 0,
    private var interceptions: Int = 0
) {

    fun withId(id: Long) = apply {
        this.id = id
    }

    fun withPlayerName(playerName: String) = apply {
        this.playerName = playerName
    }

    fun withPosition(position: String) = apply {
        this.position = position
    }

    fun withDateOfBirth(dateOfBirth: String) = apply {
        this.dateOfBirth = dateOfBirth
    }

    fun withNationality(nationality: String) = apply {
        this.nationality = nationality
    }

    fun withShoots(shoots: Int) = apply {
        this.shoots = shoots
    }

    fun withInterceptions(interceptions: Int) = apply {
        this.interceptions = interceptions
    }

    fun build(): Player {
        val player = Player(
            id = id,
            playerName = playerName,
            position = position,
            dateOfBirth = dateOfBirth,
            nationality = nationality,
            shoots = shoots,
            interceptions = interceptions
        )
        return player
    }
}