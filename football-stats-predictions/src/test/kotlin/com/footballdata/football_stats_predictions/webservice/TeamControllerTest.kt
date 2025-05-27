package com.footballdata.football_stats_predictions.webservice

import com.footballdata.football_stats_predictions.model.Match
import com.footballdata.football_stats_predictions.model.Player
import com.footballdata.football_stats_predictions.service.QueryHistoryService
import com.footballdata.football_stats_predictions.service.TeamService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
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

        val authentication: Authentication = org.mockito.Mockito.mock(Authentication::class.java)
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
    fun `getTeamComposition should propagate exception when service fails`() {
        // Arrange
        val teamName = "Barcelona"
        `when`(teamService.getTeamComposition(teamName))
            .thenThrow(RuntimeException("Service error"))

        val authentication: Authentication = org.mockito.Mockito.mock(Authentication::class.java)
        `when`(authentication.name).thenReturn("123")

        // Act & Assert
        assertThrows<Exception> {
            teamController.getTeamComposition(teamName, authentication)
        }
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

        val authentication: Authentication = org.mockito.Mockito.mock(Authentication::class.java)
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

        val authentication: Authentication = org.mockito.Mockito.mock(Authentication::class.java)
        `when`(authentication.name).thenReturn("123")

        // Act
        val result = teamController.getScheduledMatches(teamName, authentication)

        // Assert
        assertEquals(expectedMatches, result.body)
        verify(teamService).getScheduledMatches(teamName)
    }
}