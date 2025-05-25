package com.footballdata.football_stats_predictions.service

import com.footballdata.football_stats_predictions.data.FootballDataAPI
import com.footballdata.football_stats_predictions.model.Match
import com.footballdata.football_stats_predictions.model.PlayerBuilder
import com.footballdata.football_stats_predictions.model.Team
import com.footballdata.football_stats_predictions.repositories.PlayerRepository
import com.footballdata.football_stats_predictions.repositories.TeamRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
class TeamServiceTest {

    @Mock
    private lateinit var footballDataAPI: FootballDataAPI

    @Mock
    private lateinit var playerRepository: PlayerRepository

    @Mock
    private lateinit var teamRepository: TeamRepository

    private lateinit var teamService: TeamService

    @BeforeEach
    fun setup() {
        teamService = TeamService(footballDataAPI, playerRepository, teamRepository)
    }

    @Test
    fun `should return cached players when team exists in database`() {
        // Arrange
        val teamName = "Real Madrid"
        val cachedPlayers = listOf(
            PlayerBuilder().withPlayerName("Jugador1").build(),
            PlayerBuilder().withPlayerName("Jugador2").build()
        )
        val cachedTeam = Team(teamName = teamName, players = cachedPlayers.toMutableList())

        `when`(teamRepository.findByTeamName(teamName)).thenReturn(cachedTeam)

        // Act
        val result = teamService.getTeamComposition(teamName)

        // Assert
        assert(result == cachedPlayers)
        verify(footballDataAPI, never()).getTeamComposition(any())
        verify(teamRepository, times(1)).findByTeamName(teamName)
    }

    @Test
    fun `should fetch from API and save when team does not exist`() {
        // Arrange
        val teamName = "Barcelona"
        val apiPlayers = listOf(
            PlayerBuilder().withPlayerName("Jugador3").build(),
            PlayerBuilder().withPlayerName("Jugador4").build()
        )

        `when`(teamRepository.findByTeamName(teamName)).thenReturn(null)
        `when`(footballDataAPI.getTeamComposition(teamName)).thenReturn(apiPlayers)
        `when`(playerRepository.findByPlayerName(any())).thenReturn(null)

        // Act
        val result = teamService.getTeamComposition(teamName)

        // Assert
        assert(result == apiPlayers)
        verify(teamRepository, times(1)).findByTeamName(teamName)
        verify(footballDataAPI, times(1)).getTeamComposition(teamName)
        verify(teamRepository, times(1)).save(any())
        verify(playerRepository, times(2)).save(any())
    }

    @Test
    fun `should not save existing players when fetching from API`() {
        // Arrange
        val teamName = "Barcelona"
        val existingPlayer = PlayerBuilder()
            .withPlayerName("Jugador3")
            .withPosition("Forward")
            .build()
        val apiPlayers = listOf(existingPlayer)

        `when`(teamRepository.findByTeamName(teamName)).thenReturn(null)
        `when`(footballDataAPI.getTeamComposition(teamName)).thenReturn(apiPlayers)
        `when`(playerRepository.findByPlayerName("Jugador3")).thenReturn(existingPlayer)

        // Act
        teamService.getTeamComposition(teamName)

        // Assert
        verify(playerRepository, never()).save(existingPlayer)
    }

    @Test
    fun `should return scheduled matches from API`() {
        // Arrange
        val teamName = "Barcelona"
        val expectedMatches = listOf(
            Match(
                id = 1,
                league = "La Liga",
                date = "2024-03-20T20:00:00Z",
                homeTeamId = 81,
                homeTeamName = "Barcelona",
                awayTeamId = 86,
                awayTeamName = "Real Madrid"
            ),
            Match(
                id = 2,
                league = "Champions League",
                date = "2024-03-25T20:00:00Z",
                homeTeamId = 81,
                homeTeamName = "Barcelona",
                awayTeamId = 50,
                awayTeamName = "Manchester City"
            )
        )

        `when`(footballDataAPI.getScheduledMatches(teamName)).thenReturn(expectedMatches)

        // Act
        val result = teamService.getScheduledMatches(teamName)

        // Assert
        assert(result == expectedMatches)
        verify(footballDataAPI, times(1)).getScheduledMatches(teamName)
    }

    @Test
    fun `should return empty list when no scheduled matches`() {
        // Arrange
        val teamName = "Barcelona"
        val expectedMatches = emptyList<Match>()

        `when`(footballDataAPI.getScheduledMatches(teamName)).thenReturn(expectedMatches)

        // Act
        val result = teamService.getScheduledMatches(teamName)

        // Assert
        assert(result.isEmpty())
        verify(footballDataAPI, times(1)).getScheduledMatches(teamName)
    }

    @Test
    fun `should propagate exception when API fails`() {
        // Arrange
        val teamName = "Barcelona"
        `when`(footballDataAPI.getScheduledMatches(teamName)).thenThrow(RuntimeException("API Error"))

        // Act & Assert
        assertThrows<RuntimeException> {
            teamService.getScheduledMatches(teamName)
        }
        verify(footballDataAPI, times(1)).getScheduledMatches(teamName)
    }
}