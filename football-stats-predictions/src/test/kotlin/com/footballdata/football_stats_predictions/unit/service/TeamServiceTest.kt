package com.footballdata.football_stats_predictions.unit.service

import com.footballdata.football_stats_predictions.data.FootballDataAPI
import com.footballdata.football_stats_predictions.data.TeamScraper
import com.footballdata.football_stats_predictions.model.Match
import com.footballdata.football_stats_predictions.model.PlayerBuilder
import com.footballdata.football_stats_predictions.model.Team
import com.footballdata.football_stats_predictions.repositories.PlayerRepository
import com.footballdata.football_stats_predictions.repositories.TeamRepository
import com.footballdata.football_stats_predictions.service.StatsAnalyzer
import com.footballdata.football_stats_predictions.service.TeamService
import org.junit.jupiter.api.Assertions.assertEquals
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
    private lateinit var teamScraper: TeamScraper

    @Mock
    private lateinit var playerRepository: PlayerRepository

    @Mock
    private lateinit var teamRepository: TeamRepository

    @Mock
    private lateinit var statsAnalyzer: StatsAnalyzer

    private lateinit var teamService: TeamService

    @BeforeEach
    fun setup() {
        teamService = TeamService(footballDataAPI, teamScraper, playerRepository, teamRepository, statsAnalyzer)
    }

    @Test
    fun `should return cached players when team exists in database`() {
        // Arrange
        val teamName = "Real Madrid"
        val expectedCachedPlayers = listOf(
            PlayerBuilder().withPlayerName("Jugador1").build(),
            PlayerBuilder().withPlayerName("Jugador2").build()
        )
        val cachedTeam = Team(teamName = teamName, players = expectedCachedPlayers.toMutableList())

        `when`(teamRepository.findByTeamName(teamName)).thenReturn(cachedTeam)

        // Act
        val result = teamService.getTeamComposition(teamName)

        // Assert
        assertEquals(expectedCachedPlayers, result)
        verify(footballDataAPI, never()).getTeamComposition(any())
        verify(teamRepository, times(1)).findByTeamName(teamName)
    }

    @Test
    fun `should fetch from API and save when team does not exist`() {
        // Arrange
        val teamName = "Barcelona"
        val expectedApiPlayers = listOf(
            PlayerBuilder().withPlayerName("Jugador3").build(),
            PlayerBuilder().withPlayerName("Jugador4").build()
        )

        `when`(teamRepository.findByTeamName(teamName)).thenReturn(null)
        `when`(footballDataAPI.getTeamComposition(teamName)).thenReturn(expectedApiPlayers)
        `when`(playerRepository.findByPlayerName(any())).thenReturn(null)

        // Act
        val result = teamService.getTeamComposition(teamName)

        // Assert
        assertEquals(expectedApiPlayers, result)
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
        assertEquals(expectedMatches, result)
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

    @Test
    fun `should return team statistics from scraping service`() {
        // Arrange
        val teamName = "Barcelona"
        val expectedStats = mapOf(
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

        `when`(teamScraper.getTeamData(teamName)).thenReturn(expectedStats)

        // Act
        val result = teamService.getTeamStatistics(teamName)

        // Assert
        assertEquals(expectedStats, result)
        verify(teamScraper, times(1)).getTeamData(teamName)
    }

    @Test
    fun `should return empty map when no team statistics available`() {
        // Arrange
        val teamName = "UnknownTeam"
        val emptyStats = emptyMap<String, Double>()

        `when`(teamScraper.getTeamData(teamName)).thenReturn(emptyStats)

        // Act
        val result = teamService.getTeamStatistics(teamName)

        // Assert
        assert(result.isEmpty())
        verify(teamScraper, times(1)).getTeamData(teamName)
    }

    @Test
    fun `should return advanced team statistics from scraping service`() {
        // Arrange
        val teamName = "Real Madrid"
        val expectedAdvancedStats = mapOf(
            "Goles por Partido" to 2.19,
            "Efectividad de Tiros" to 7.35,
            "Ganados" to 42.0,
            "Empatados" to 8.0,
            "Perdidos" to 13.0
        )

        `when`(teamScraper.getTeamAdvancedStatistics(teamName)).thenReturn(expectedAdvancedStats)

        // Act
        val result = teamService.getTeamAdvancedStatistics(teamName)

        // Assert
        assertEquals(expectedAdvancedStats, result)
        verify(teamScraper, times(1)).getTeamAdvancedStatistics(teamName)
    }

    @Test
    fun `should propagate exception when scraping fails for advanced statistics`() {
        // Arrange
        val teamName = "Barcelona"
        `when`(teamScraper.getTeamAdvancedStatistics(teamName))
            .thenThrow(RuntimeException("Scraping Error"))

        // Act & Assert
        assertThrows<RuntimeException> {
            teamService.getTeamAdvancedStatistics(teamName)
        }
        verify(teamScraper, times(1)).getTeamAdvancedStatistics(teamName)
    }

    @Test
    fun `should return match prediction probabilities from scraping service`() {
        // Arrange
        val localTeam = "Barcelona"
        val awayTeam = "Real Madrid"
        val expectedProbabilities = mapOf(
            "Local Win" to 45.2,
            "Draw" to 28.5,
            "Visiting Win" to 26.3
        )

        `when`(teamScraper.predictMatchProbabilities(localTeam, awayTeam))
            .thenReturn(expectedProbabilities)

        // Act
        val result = teamService.predictMatchProbabilities(localTeam, awayTeam)

        // Assert
        assertEquals(expectedProbabilities, result)
        verify(teamScraper, times(1)).predictMatchProbabilities(localTeam, awayTeam)
    }

    @Test
    fun `should handle equal probability predictions`() {
        // Arrange
        val localTeam = "Valencia"
        val awayTeam = "Sevilla"
        val expectedEqualProbabilities = mapOf(
            "Local Win" to 33.3,
            "Draw" to 33.4,
            "Visiting Win" to 33.3
        )

        `when`(teamScraper.predictMatchProbabilities(localTeam, awayTeam))
            .thenReturn(expectedEqualProbabilities)

        // Act
        val result = teamService.predictMatchProbabilities(localTeam, awayTeam)

        // Assert
        assertEquals(expectedEqualProbabilities, result)
        verify(teamScraper, times(1)).predictMatchProbabilities(localTeam, awayTeam)
    }

    @Test
    fun `should return team comparison data from scraping service`() {
        // Arrange
        val localTeam = "Barcelona"
        val awayTeam = "Real Madrid"
        val expectedComparison = mapOf(
            "barcelona" to mapOf(
                "Apps" to "60.0 (-3.00)",
                "Goles" to "174.0 (36.00)",
                "Tiros pp" to "17.3 (1.20)",
                "Yellow Cards" to "89.0 (-22.00)",
                "Red Cards" to "6.0 (-3.00)",
                "Posesion%" to "67.4 (9.10)",
                "AciertoPase%" to "88.4 (-1.40)",
                "Aéreos" to "10.1 (1.80)",
                "Rating" to "6.86 (0.02)"
            ),
            "real madrid" to mapOf(
                "Apps" to "63.0 (3.00)",
                "Goles" to "138.0 (-36.00)",
                "Tiros pp" to "16.1 (-1.20)",
                "Yellow Cards" to "111.0 (22.00)",
                "Red Cards" to "9.0 (3.00)",
                "Posesion%" to "58.3 (-9.10)",
                "AciertoPase%" to "89.8 (1.40)",
                "Aéreos" to "8.3 (-1.80)",
                "Rating" to "6.84 (-0.02)"
            )
        )

        `when`(teamScraper.compareTeamStatsWithDiff(localTeam, awayTeam))
            .thenReturn(expectedComparison)

        // Act
        val result = teamService.compareTeams(localTeam, awayTeam)

        // Assert
        assertEquals(expectedComparison, result)
        verify(teamScraper, times(1)).compareTeamStatsWithDiff(localTeam, awayTeam)
    }

    @Test
    fun `should handle empty team comparison when teams not found`() {
        // Arrange
        val localTeam = "UnknownTeam1"
        val awayTeam = "UnknownTeam2"
        val emptyComparison = emptyMap<String, Map<String, String>>()

        `when`(teamScraper.compareTeamStatsWithDiff(localTeam, awayTeam))
            .thenReturn(emptyComparison)

        // Act
        val result = teamService.compareTeams(localTeam, awayTeam)

        // Assert
        assert(result.isEmpty())
        verify(teamScraper, times(1)).compareTeamStatsWithDiff(localTeam, awayTeam)
    }

    @Test
    fun `should propagate exception when scraping fails for team comparison`() {
        // Arrange
        val localTeam = "Barcelona"
        val awayTeam = "Real Madrid"
        `when`(teamScraper.compareTeamStatsWithDiff(localTeam, awayTeam))
            .thenThrow(RuntimeException("Comparison scraping failed"))

        // Act & Assert
        assertThrows<RuntimeException> {
            teamService.compareTeams(localTeam, awayTeam)
        }
        verify(teamScraper, times(1)).compareTeamStatsWithDiff(localTeam, awayTeam)
    }
}
