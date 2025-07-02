package com.footballdata.football_stats_predictions.model

import jakarta.persistence.*

@Entity
class Team(
    @Id
    val id: Long = 0,
    val teamName: String,

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    var players: MutableList<Player> = mutableListOf()
)

class TeamBuilder(
    private var id: Long = 0,
    private var teamName: String = "",
    private var players: MutableList<Player> = mutableListOf()
) {
    fun withId(id: Long) = apply { this.id = id }
    fun withTeamName(teamName: String) = apply { this.teamName = teamName }
    fun withPlayers(players: MutableList<Player>) = apply { this.players = players }

    fun build() = Team(
        id = id,
        teamName = teamName,
        players = players
    )
}