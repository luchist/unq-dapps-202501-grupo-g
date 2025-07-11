package com.footballdata.football_stats_predictions.unit.service

import com.footballdata.football_stats_predictions.data.FootballDataAPI
import com.footballdata.football_stats_predictions.data.TeamScraper
import com.footballdata.football_stats_predictions.model.*
import com.footballdata.football_stats_predictions.repositories.*
import com.footballdata.football_stats_predictions.service.TeamService
import com.footballdata.football_stats_predictions.utils.PersistenceHelper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

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

    private lateinit var teamService: TeamService

    @Mock
    private lateinit var teamStatsRepository: TeamStatsRepository

    @Mock
    private lateinit var comparisonRepository: ComparisonRepository

    @Mock
    private lateinit var matchPredictionRepository: MatchPredictionRepository

    @Mock
    private lateinit var persistenceHelper: PersistenceHelper

    @BeforeEach
    fun setup() {
        teamService = TeamService(
            footballDataAPI,
            teamScraper,
            playerRepository,
            teamRepository,
            teamStatsRepository,
            comparisonRepository,
            matchPredictionRepository,
            persistenceHelper
        )    }

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
        val teamId = "86"
        val teamName = "Barcelona"
        val expectedApiPlayers = listOf(
            PlayerBuilder().withPlayerName("Jugador1").build(),
            PlayerBuilder().withPlayerName("Jugador2").build()
        )

        `when`(teamRepository.findByTeamName(eq(teamId))).thenReturn(null)
        `when`(footballDataAPI.getTeamComposition(eq(teamId))).thenReturn(expectedApiPlayers)
        `when`(footballDataAPI.getTeamName(eq(teamId))).thenReturn(teamName)
        `when`(teamRepository.save(any())).thenAnswer { it.getArgument(0) }

        // Act
        val result = teamService.getTeamComposition(teamId)

        // Assert
        assertEquals(expectedApiPlayers, result)
        verify(teamRepository, times(1)).findByTeamName(eq(teamId))
        verify(footballDataAPI, times(1)).getTeamComposition(eq(teamId))
        verify(footballDataAPI, times(1)).getTeamName(eq(teamId))
        verify(teamRepository, times(1)).save(any())
        verify(playerRepository, times(2)).findByPlayerName(any())
        verify(playerRepository, times(2)).save(any())
    }

    @Test
    fun `should not save existing players when fetching from API`() {
        // Arrange
        val teamId = "86"
        val teamName = "Barcelona"
        val existingPlayer = PlayerBuilder().withPlayerName("Jugador3").build()
        val newPlayer = PlayerBuilder().withPlayerName("Jugador4").build()
        val expectedApiPlayers = listOf(existingPlayer, newPlayer)

        `when`(teamRepository.findByTeamName(eq(teamId))).thenReturn(null)
        `when`(footballDataAPI.getTeamComposition(eq(teamId))).thenReturn(expectedApiPlayers)
        `when`(footballDataAPI.getTeamName(eq(teamId))).thenReturn(teamName)
        `when`(playerRepository.findByPlayerName(eq(existingPlayer.playerName))).thenReturn(existingPlayer)
        `when`(playerRepository.findByPlayerName(eq(newPlayer.playerName))).thenReturn(null)

        // Act
        val result = teamService.getTeamComposition(teamId)

        // Assert
        assert(result == expectedApiPlayers)
        verify(teamRepository, times(1)).findByTeamName(eq(teamId))
        verify(footballDataAPI, times(1)).getTeamComposition(eq(teamId))
        verify(footballDataAPI, times(1)).getTeamName(eq(teamId))
        verify(teamRepository, times(1)).save(any<Team>())
        verify(playerRepository, times(1)).save(any<Player>())
        verify(playerRepository, times(2)).findByPlayerName(any())
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
        val expectedStats = TeamStatsBuilder()
            .withTeamName(teamName)
            .withData(mapOf(
                "Apps" to 25.0,
                "Goles" to 47.0,
                "Tiros pp" to 12.8,
                "Yellow Cards" to 44.0,
                "Red Cards" to 0.0,
                "Posesion%" to 54.5,
                "AciertoPase%" to 86.0,
                "Aéreos" to 12.4,
                "Rating" to 6.62
            ))
            .build()

        `when`(persistenceHelper.getCachedOrFetch(
            eq(teamStatsRepository),
            any<() -> TeamStats?>(),
            any<() -> Any>(),
            any<(Any) -> TeamStats>()
        )).thenReturn(expectedStats)

        // Act
        val result = teamService.getTeamStatistics(teamName)

        // Assert
        assert(result == expectedStats)
        verify(persistenceHelper, times(1)).getCachedOrFetch(
            eq(teamStatsRepository),
            any<() -> TeamStats?>(),
            any<() -> Any>(),
            any<(Any) -> TeamStats>()
        )
    }

    @Test
    fun `should return empty map when no team statistics available`() {
        // Arrange
        val teamName = "Unknown Team"
        val emptyStats = TeamStatsBuilder()
            .withTeamName(teamName)
            .withData(emptyMap())
            .build()

        `when`(persistenceHelper.getCachedOrFetch(
            eq(teamStatsRepository),
            any(),
            any(),
            any()
        )).thenReturn(emptyStats)

        // Act
        val result = teamService.getTeamStatistics(teamName)

        // Assert
        assert(result.isEmpty())

        verify(persistenceHelper, times(1)).getCachedOrFetch(
            eq(teamStatsRepository),
            any(),
            any(),
            any()
        )
    }

    @Test
    fun `should return advanced team statistics from scraping service`() {
        // Arrange
        val teamName = "Real Madrid"
        val advancedTeamName = "advanced_$teamName"
        val expectedAdvancedStats = TeamStatsBuilder()
            .withTeamName(advancedTeamName)
            .withData(mapOf(
                "Goals per game" to 2.19,
                "Shot Effectiveness" to 7.35,
                "Wins" to 42.0,
                "Draws" to 8.0,
                "Losses" to 13.0
            ))
            .build()

        `when`(persistenceHelper.getCachedOrFetch(
            eq(teamStatsRepository),
            any<() -> TeamStats?>(),
            any<() -> Any>(),
            any<(Any) -> TeamStats>()
        )).thenReturn(expectedAdvancedStats)

        // Act
        val result = teamService.getTeamAdvancedStatistics(teamName)

        // Assert
        assert(result == expectedAdvancedStats)
        verify(persistenceHelper, times(1)).getCachedOrFetch(
            eq(teamStatsRepository),
            any<() -> TeamStats?>(),
            any<() -> Any>(),
            any<(Any) -> TeamStats>()
        )
    }

    @Test
    fun `should propagate exception when scraping fails for advanced statistics`() {
        // Arrange
        val teamName = "Barcelona"

        `when`(teamScraper.getTeamAdvancedStatistics(eq(teamName)))
            .thenThrow(RuntimeException("Error getting advanced statistics"))

        `when`(persistenceHelper.getCachedOrFetch(
            eq(teamStatsRepository),
            any(),
            any<() -> Any>(),
            any()
        )).thenAnswer { invocation ->
            val fetchFunction = invocation.getArgument<() -> Any>(2)
            fetchFunction.invoke()
        }

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
        val prediction = MatchPrediction(
            localTeam = localTeam,
            awayTeam = awayTeam,
            predictions = expectedProbabilities
        )

        `when`(persistenceHelper.getCachedOrFetch(
            eq(matchPredictionRepository),
            any<() -> MatchPrediction?>(),
            any<() -> Any>(),
            any<(Any) -> MatchPrediction>()
        )).thenReturn(prediction)

        // Act
        val result = teamService.predictMatchProbabilities(localTeam, awayTeam)

        // Assert
        assert(result == expectedProbabilities)
        verify(persistenceHelper, times(1)).getCachedOrFetch(
            eq(matchPredictionRepository),
            any<() -> MatchPrediction?>(),
            any<() -> Any>(),
            any<(Any) -> MatchPrediction>()
        )
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
        val prediction = MatchPrediction(
            localTeam = localTeam,
            awayTeam = awayTeam,
            predictions = expectedEqualProbabilities
        )

        `when`(persistenceHelper.getCachedOrFetch(
            eq(matchPredictionRepository),
            any<() -> MatchPrediction?>(),
            any<() -> Any>(),
            any<(Any) -> MatchPrediction>()
        )).thenReturn(prediction)

        // Act
        val result = teamService.predictMatchProbabilities(localTeam, awayTeam)

        // Assert
        assert(result == expectedEqualProbabilities)
        verify(persistenceHelper, times(1)).getCachedOrFetch(
            eq(matchPredictionRepository),
            any<() -> MatchPrediction?>(),
            any<() -> Any>(),
            any<(Any) -> MatchPrediction>()
        )
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
                "Rating" to "6.86 (0.02)"
            ),
            "real madrid" to mapOf(
                "Apps" to "63.0 (3.00)",
                "Goles" to "138.0 (-36.00)",
                "Rating" to "6.84 (-0.02)"
            )
        )
        val comparison = Comparison(
            comparisonType = ComparisonType.TEAM,
            entity1Name = localTeam,
            entity2Name = awayTeam,
            comparisonData = expectedComparison
        )

        `when`(persistenceHelper.getCachedOrFetch(
            eq(comparisonRepository),
            any<() -> Comparison?>(),
            any<() -> Any>(),
            any<(Any) -> Comparison>()
        )).thenReturn(comparison)

        // Act
        val result = teamService.compareTeams(localTeam, awayTeam)

        // Assert
        assert(result == expectedComparison)
        verify(persistenceHelper, times(1)).getCachedOrFetch(
            eq(comparisonRepository),
            any<() -> Comparison?>(),
            any<() -> Any>(),
            any<(Any) -> Comparison>()
        )
    }

    @Test
    fun `should handle empty team comparison when teams not found`() {
        // Arrange
        val localTeam = "UnknownTeam1"
        val awayTeam = "UnknownTeam2"
        val emptyComparison = emptyMap<String, Map<String, String>>()
        val comparison = Comparison(
            comparisonType = ComparisonType.TEAM,
            entity1Name = localTeam,
            entity2Name = awayTeam,
            comparisonData = emptyComparison
        )

        `when`(persistenceHelper.getCachedOrFetch(
            eq(comparisonRepository),
            any<() -> Comparison?>(),
            any<() -> Any>(),
            any<(Any) -> Comparison>()
        )).thenReturn(comparison)

        // Act
        val result = teamService.compareTeams(localTeam, awayTeam)

        // Assert
        assert(result.isEmpty())
        verify(persistenceHelper, times(1)).getCachedOrFetch(
            eq(comparisonRepository),
            any<() -> Comparison?>(),
            any<() -> Any>(),
            any<(Any) -> Comparison>()
        )
    }

    @Test
    fun `should propagate exception when scraping fails for team comparison`() {
        // Arrange
        val localTeam = "Barcelona"
        val awayTeam = "Real Madrid"

        `when`(persistenceHelper.getCachedOrFetch(
            eq(comparisonRepository),
            any<() -> Comparison?>(),
            any<() -> Any>(),
            any<(Any) -> Comparison>()
        )).thenThrow(RuntimeException("Comparison scraping failed"))

        // Act & Assert
        assertThrows<RuntimeException> {
            teamService.compareTeams(localTeam, awayTeam)
        }
        verify(persistenceHelper, times(1)).getCachedOrFetch(
            eq(comparisonRepository),
            any<() -> Comparison?>(),
            any<() -> Any>(),
            any<(Any) -> Comparison>()
        )
    }
}
