package com.footballdata.football_stats_predictions.service

import com.footballdata.football_stats_predictions.data.FootballDataAPI
import com.footballdata.football_stats_predictions.model.Player
import com.footballdata.football_stats_predictions.model.TeamBuilder
import com.footballdata.football_stats_predictions.repositories.PlayerRepository
import com.footballdata.football_stats_predictions.repositories.TeamRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TeamService(
    @field:Autowired var footballDataAPI: FootballDataAPI,
    @field:Autowired var playerRepository: PlayerRepository,
    @field:Autowired var teamRepository: TeamRepository
) {

    fun getTeamComposition(teamName: String): List<Player> {

        // Check if the team exists in the database
        val cachedTeam = teamRepository.findByTeamName(teamName)

        // If the team exists, fetch the players from the database
        if (cachedTeam != null) {
            return cachedTeam.players.toList()
        }

        // If the team does not exist, fetch the players from the API
        val teamComposition = footballDataAPI.getTeamComposition(teamName)

        // Save the team to the database
        val team = TeamBuilder()
            .withTeamName(teamName)
            .withPlayers(teamComposition.toMutableList())
            .build()

        teamRepository.save(team)

        // Save each player to the database
        for (player in teamComposition) {
            val existingPlayer = playerRepository.findByPlayerName(player.playerName)
            if (existingPlayer == null) {
                playerRepository.save(player)
            }
        }

        // return the team composition
        return teamComposition
    }
}