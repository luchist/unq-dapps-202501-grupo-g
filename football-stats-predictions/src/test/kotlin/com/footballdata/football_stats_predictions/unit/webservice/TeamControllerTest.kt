package com.footballdata.football_stats_predictions.unit.webservice

import com.footballdata.football_stats_predictions.model.Match
import com.footballdata.football_stats_predictions.model.Player
import com.footballdata.football_stats_predictions.model.TeamStats
import com.footballdata.football_stats_predictions.service.QueryHistoryService
import com.footballdata.football_stats_predictions.service.TeamService
import com.footballdata.football_stats_predictions.webservice.TeamController
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.core.Authentication

@ExtendWith(MockitoExtension::class)
class TeamControllerTest {

    @Mock
    private lateinit var queryHistoryService: QueryHistoryService

    @Mock
    private lateinit var teamService: TeamService

    @InjectMocks
    private lateinit var teamController: TeamController

    @Test
    fun `getTeamComposition should return list of players`() {
        // Arrange
        val teamName = "Barcelona"

        val authentication: Authentication = Mockito.mock(Authentication::class.java)
        `when`(authentication.name).thenReturn("123")

        val expectedPlayers = listOf(
            Player(
                1L, "Lionel Messi", "Forward",
                "1987-06-24", "Argentina", 10, 10
            ),
            Player(
                2L, "Pedri", "Midfielder",
                "2002-11-25", "Spain", 5, 5
            ),
        )
        `when`(teamService.getTeamComposition(teamName)).thenReturn(expectedPlayers)

        // Act
        val result = teamController.getTeamComposition(teamName, authentication)

        // Assert
        assertEquals(expectedPlayers, result.body)
        verify(teamService).getTeamComposition(teamName)
    }

    @Test
    fun `getTeamComposition should return 404 when service fails`() {
        // Arrange
        val teamName = "Barcelona"
        `when`(teamService.getTeamComposition(teamName))
            .thenThrow(RuntimeException("Service error"))

        val authentication: Authentication = Mockito.mock(Authentication::class.java)
        `when`(authentication.name).thenReturn("123")

        // Act
        val result = teamController.getTeamComposition(teamName, authentication)

        // Assert
        assertEquals(404, result.statusCode.value())
        verify(teamService).getTeamComposition(teamName)
    }

    @Test
    fun `getScheduledMatches should return list of matches`() {
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
            )
        )
        `when`(teamService.getScheduledMatches(teamName)).thenReturn(expectedMatches)

        val authentication: Authentication = Mockito.mock(Authentication::class.java)
        `when`(authentication.name).thenReturn("123")

        // Act
        val result = teamController.getScheduledMatches(teamName, authentication)

        // Assert
        assertEquals(expectedMatches, result.body)
        verify(teamService).getScheduledMatches(teamName)
    }

    @Test
    fun `getScheduledMatches should handle empty list`() {
        // Arrange
        val teamName = "Barcelona"
        val expectedMatches = emptyList<Match>()
        `when`(teamService.getScheduledMatches(teamName)).thenReturn(expectedMatches)

        val authentication: Authentication = Mockito.mock(Authentication::class.java)
        `when`(authentication.name).thenReturn("123")

        // Act
        val result = teamController.getScheduledMatches(teamName, authentication)

        // Assert
        assertEquals(expectedMatches, result.body)
        verify(teamService).getScheduledMatches(teamName)
    }

    @Test
    fun `getTeamStats should return team statistics`() {
        // Arrange
        val teamName = "Barcelona"
        val expectedStats = TeamStats(
            mapOf(
                "Apps" to 25.0,
                "Goles" to 47.0,
                "Tiros pp" to 12.8,
                "Yellow Cards" to 44.0,
                "Red Cards" to 0.0,
                "Posesion%" to 54.5,
                "AciertoPase%" to 86.0,
                "Aéreos" to 12.4,
                "Rating" to 6.62
            )
        )

        `when`(teamService.getTeamStatistics(teamName)).thenReturn(expectedStats)

        // Act
        val result = teamController.getTeamStats(teamName)

        // Assert
        assertEquals(expectedStats, result)
        verify(teamService).getTeamStatistics(teamName)
    }

    @Test
    fun `getTeamAdvancedStatistics should return advanced team statistics`() {
        // Arrange
        val teamName = "Barcelona"
        val expectedAdvancedStats = TeamStats(mapOf(
            "Goles por Partido" to 1.3846153846153846,
            "Efectividad de Tiros" to 13.505555555555555,
            "Ganados" to 25.0,
            "Empatados" to 12.0,
            "Perdidos" to 13.0
        ))

        `when`(teamService.getTeamAdvancedStatistics(teamName)).thenReturn(expectedAdvancedStats)

        // Act
        val result = teamController.getTeamAdvancedStatistics(teamName)

        // Assert
        assertEquals(expectedAdvancedStats, result)
        verify(teamService).getTeamAdvancedStatistics(teamName)
    }

    @Test
    fun `predictMatchProbabilities should return match prediction probabilities`() {
        // Arrange
        val localTeam = "Barcelona"
        val awayTeam = "Real Madrid"
        val expectedProbabilities = mapOf(
            "Local Win" to 45.2,
            "Draw" to 28.5,
            "Visiting Win" to 26.3
        )
        `when`(teamService.predictMatchProbabilities(localTeam, awayTeam)).thenReturn(expectedProbabilities)

        // Act
        val result = teamController.predictMatchProbabilities(localTeam, awayTeam)

        // Assert
        assertEquals(expectedProbabilities, result)
        verify(teamService).predictMatchProbabilities(localTeam, awayTeam)
    }

    @Test
    fun `compareTeams should return team comparison data`() {
        // Arrange
        val localTeam = "Barcelona"
        val awayTeam = "Real Madrid"
        val expectedComparison = mapOf(
            "boca juniors" to mapOf(
                "Apps" to "13.0 (-41.00)",
                "Goles" to "18.0 (-132.00)",
                "Tiros pp" to "18.7 (-0.70)",
                "Yellow Cards" to "29.0 (-44.00)",
                "Red Cards" to "5.0 (2.00)",
                "Posesion%" to "47.1 (-20.70)",
                "AciertoPase%" to "81.6 (-8.20)",
                "Aéreos" to "14.3 (2.00)",
                "Rating" to "6.58 (-0.35)"
            ),
            "bayern munich" to mapOf(
                "Apps" to "54.0 (41.00)",
                "Goles" to "150.0 (132.00)",
                "Tiros pp" to "19.4 (0.70)",
                "Yellow Cards" to "73.0 (44.00)",
                "Red Cards" to "3.0 (-2.00)",
                "Posesion%" to "67.8 (20.70)",
                "AciertoPase%" to "89.8 (8.20)",
                "Aéreos" to "12.3 (-2.00)",
                "Rating" to "6.93 (0.35)"
            ),
        )

        `when`(teamService.compareTeams(localTeam, awayTeam)).thenReturn(expectedComparison)

        // Act
        val result = teamController.compareTeams(localTeam, awayTeam)

        // Assert
        assertEquals(expectedComparison, result)
        verify(teamService).compareTeams(localTeam, awayTeam)
    }
}