package com.footballdata.football_stats_predictions.unit.model

import com.footballdata.football_stats_predictions.model.PlayerBuilder
import com.footballdata.football_stats_predictions.model.TeamBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TeamTest {

    @Test
    fun `should create team with correct attributes when using builder`() {
        // Arrange
        val expectedTeamName = "Real Madrid"
        val expectedPlayers = listOf(
            PlayerBuilder()
                .withPlayerName("Cristiano Ronaldo")
                .withPosition("Forward")
                .withShoots(10)
                .withInterceptions(2)
                .withDateOfBirth("05/02/1985")
                .withNationality("Portugal")
                .build(),
            PlayerBuilder()
                .withPlayerName("Lionel Messi")
                .withPosition("Forward")
                .withShoots(12)
                .withInterceptions(3)
                .withDateOfBirth("24/06/1987")
                .withNationality("Argentina")
                .build()
        ).toMutableList()

        // Act
        val team = TeamBuilder()
            .withTeamName(expectedTeamName)
            .withPlayers(expectedPlayers)
            .build()

        // Assert
        assertEquals(expectedTeamName, team.teamName)
        assertEquals(expectedPlayers, team.players)
        assertEquals(2, team.players.size)
        assertEquals("Cristiano Ronaldo", team.players[0].playerName)
        assertEquals("Lionel Messi", team.players[1].playerName)
    }

    @Test
    fun `should create team with empty player list when using builder without players`() {
        // Arrange
        val expectedTeamName = "Barcelona"

        // Act
        val team = TeamBuilder()
            .withTeamName(expectedTeamName)
            .build()

        // Assert
        assertEquals(expectedTeamName, team.teamName)
        assertEquals(0, team.players.size)
    }

    @Test
    fun `should allow adding players after team creation`() {
        // Arrange
        val team = TeamBuilder()
            .withTeamName("Valencia")
            .build()
        val newPlayer = PlayerBuilder()
            .withPlayerName("David Villa")
            .build()

        // Act
        team.players.add(newPlayer)

        // Assert
        assertEquals(1, team.players.size)
        assertEquals("David Villa", team.players[0].playerName)
    }
}