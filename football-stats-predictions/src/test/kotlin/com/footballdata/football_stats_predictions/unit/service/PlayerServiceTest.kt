package com.footballdata.football_stats_predictions.unit.service

import com.footballdata.football_stats_predictions.data.PlayerScraper
import com.footballdata.football_stats_predictions.model.PlayerStatsBuilder
import com.footballdata.football_stats_predictions.repositories.ComparisonRepository
import com.footballdata.football_stats_predictions.repositories.PlayerRepository
import com.footballdata.football_stats_predictions.repositories.PlayerStatsRepository
import com.footballdata.football_stats_predictions.service.PlayerService
import com.footballdata.football_stats_predictions.utils.PersistenceHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
class PlayerServiceTest {

    @Mock
    private lateinit var playerScraper: PlayerScraper

    @Mock
    private lateinit var playerRepository: PlayerRepository

    private lateinit var playerService: PlayerService

    @Mock
    private lateinit var playerStatsRepository: PlayerStatsRepository

    @Mock
    private lateinit var comparisonRepository: ComparisonRepository

    @Mock
    private lateinit var persistenceHelper: PersistenceHelper

    @BeforeEach
    fun setup() {
        playerService = PlayerService(
            playerScraper,
            playerRepository,
            playerStatsRepository,
            comparisonRepository,
            persistenceHelper
        )    }

    @Test
    fun `should return player statistics from scraping service`() {
        // Arrange
        val playerName = "Lionel Messi"
        val expectedStats = PlayerStatsBuilder()
            .withPlayerName(playerName)
            .withData(mapOf(
                "goals" to 15.0,
                "assists" to 8.0,
                "shots_per_game" to 4.2,
                "pass_accuracy" to 89.5,
                "dribbles_completed" to 3.8,
                "minutes_played" to 2340.0,
                "key_passes" to 2.1,
                "successful_tackles" to 0.8
            ))
            .build()

        `when`(playerScraper.getPlayerData(playerName)).thenReturn(expectedStats)

        // Act
        val result = playerService.getPlayerStats(playerName)

        // Assert
        assert(result == expectedStats)
        verify(playerScraper, times(1)).getPlayerData(playerName)
    }

    @Test
    fun `should return empty map when no player statistics available`() {
        // Arrange
        val playerName = "Unknown Player"
        val emptyStats = PlayerStatsBuilder()
            .withPlayerName(playerName)
            .withData(emptyMap())
            .build()

        `when`(playerScraper.getPlayerData(playerName)).thenReturn(emptyStats)

        // Act
        val result = playerService.getPlayerStats(playerName)

        // Assert
        assert(result.isEmpty())
        verify(playerScraper, times(1)).getPlayerData(playerName)
    }

    @Test
    fun `should propagate exception when scraping fails for player statistics`() {
        // Arrange
        val playerName = "Problematic Player"
        `when`(playerScraper.getPlayerData(playerName))
            .thenThrow(RuntimeException("Scraping Error"))

        // Act & Assert
        assertThrows<RuntimeException> {
            playerService.getPlayerStats(playerName)
        }
        verify(playerScraper, times(1)).getPlayerData(playerName)
    }

    @Test
    fun `should return player ratings average from scraping service`() {
        // Arrange
        val playerName = "Pedri"
        val expectedRating = 7.8

        `when`(playerScraper.getPlayerRatingsAverage(playerName)).thenReturn(expectedRating)

        // Act
        val result = playerService.getPlayerRatingsAverage(playerName)

        // Assert
        assert(result == expectedRating)
        verify(playerScraper, times(1)).getPlayerRatingsAverage(playerName)
    }

    @Test
    fun `should return zero rating for player with no ratings`() {
        // Arrange
        val playerName = "New Player"
        val expectedRating = 0.0

        `when`(playerScraper.getPlayerRatingsAverage(playerName)).thenReturn(expectedRating)

        // Act
        val result = playerService.getPlayerRatingsAverage(playerName)

        // Assert
        assert(result == expectedRating)
        verify(playerScraper, times(1)).getPlayerRatingsAverage(playerName)
    }

    @Test
    fun `should handle high player ratings`() {
        // Arrange
        val playerName = "Cristiano Ronaldo"
        val expectedRating = 9.2

        `when`(playerScraper.getPlayerRatingsAverage(playerName)).thenReturn(expectedRating)

        // Act
        val result = playerService.getPlayerRatingsAverage(playerName)

        // Assert
        assert(result == expectedRating)
        verify(playerScraper, times(1)).getPlayerRatingsAverage(playerName)
    }

    @Test
    fun `should handle decimal rating values`() {
        // Arrange
        val playerName = "Robert Lewandowski"
        val expectedRating = 8.45

        `when`(playerScraper.getPlayerRatingsAverage(playerName)).thenReturn(expectedRating)

        // Act
        val result = playerService.getPlayerRatingsAverage(playerName)

        // Assert
        assert(result == expectedRating)
        verify(playerScraper, times(1)).getPlayerRatingsAverage(playerName)
    }

    @Test
    fun `should propagate exception when scraping fails for player ratings`() {
        // Arrange
        val playerName = "Error Player"
        `when`(playerScraper.getPlayerRatingsAverage(playerName))
            .thenThrow(RuntimeException("Rating scraping failed"))

        // Act & Assert
        assertThrows<RuntimeException> {
            playerService.getPlayerRatingsAverage(playerName)
        }
        verify(playerScraper, times(1)).getPlayerRatingsAverage(playerName)
    }

    @Test
    fun `should return player history comparison from scraping service`() {
        // Arrange
        val playerName = "Gavi"
        val year = "2023"
        val expectedComparison = mapOf(
            "goals" to mapOf(
                "2024" to "5",
                "2023" to "2",
                "difference" to "+3"
            ),
            "assists" to mapOf(
                "2024" to "4",
                "2023" to "6",
                "difference" to "-2"
            ),
            "pass_accuracy" to mapOf(
                "2024" to "91.2",
                "2023" to "87.8",
                "difference" to "+3.4"
            ),
            "minutes_played" to mapOf(
                "2024" to "1890",
                "2023" to "1245",
                "difference" to "+645"
            )
        )

        `when`(playerScraper.comparePlayerStatsWithHistory(playerName, year))
            .thenReturn(expectedComparison)

        // Act
        val result = playerService.comparePlayerHistory(playerName, year)

        // Assert
        assert(result == expectedComparison)
        verify(playerScraper, times(1)).comparePlayerStatsWithHistory(playerName, year)
    }

    @Test
    fun `should handle empty player history comparison for new player`() {
        // Arrange
        val playerName = "Young Talent"
        val year = "2023"
        val emptyComparison = emptyMap<String, Map<String, String>>()

        `when`(playerScraper.comparePlayerStatsWithHistory(playerName, year))
            .thenReturn(emptyComparison)

        // Act
        val result = playerService.comparePlayerHistory(playerName, year)

        // Assert
        assert(result.isEmpty())
        verify(playerScraper, times(1)).comparePlayerStatsWithHistory(playerName, year)
    }

    @Test
    fun `should handle identical performance across years`() {
        // Arrange
        val playerName = "Consistent Player"
        val year = "2023"
        val identicalComparison = mapOf(
            "goals" to mapOf(
                "2024" to "10",
                "2023" to "10",
                "difference" to "0"
            ),
            "assists" to mapOf(
                "2024" to "5",
                "2023" to "5",
                "difference" to "0"
            )
        )

        `when`(playerScraper.comparePlayerStatsWithHistory(playerName, year))
            .thenReturn(identicalComparison)

        // Act
        val result = playerService.comparePlayerHistory(playerName, year)

        // Assert
        assert(result == identicalComparison)
        verify(playerScraper, times(1)).comparePlayerStatsWithHistory(playerName, year)
    }

    @Test
    fun `should handle different year formats in comparison`() {
        // Arrange
        val playerName = "Frenkie de Jong"
        val year = "2022"
        val expectedComparison = mapOf(
            "goals" to mapOf(
                "2024" to "3",
                "2022" to "1",
                "difference" to "+2"
            ),
            "pass_completion_rate" to mapOf(
                "2024" to "92.1",
                "2022" to "89.7",
                "difference" to "+2.4"
            )
        )

        `when`(playerScraper.comparePlayerStatsWithHistory(playerName, year))
            .thenReturn(expectedComparison)

        // Act
        val result = playerService.comparePlayerHistory(playerName, year)

        // Assert
        assert(result == expectedComparison)
        verify(playerScraper, times(1)).comparePlayerStatsWithHistory(playerName, year)
    }

    @Test
    fun `should handle negative performance differences`() {
        // Arrange
        val playerName = "Declining Player"
        val year = "2023"
        val declineComparison = mapOf(
            "goals" to mapOf(
                "2024" to "8",
                "2023" to "15",
                "difference" to "-7"
            ),
            "minutes_played" to mapOf(
                "2024" to "1200",
                "2023" to "2500",
                "difference" to "-1300"
            )
        )

        `when`(playerScraper.comparePlayerStatsWithHistory(playerName, year))
            .thenReturn(declineComparison)

        // Act
        val result = playerService.comparePlayerHistory(playerName, year)

        // Assert
        assert(result == declineComparison)
        verify(playerScraper, times(1)).comparePlayerStatsWithHistory(playerName, year)
    }

    @Test
    fun `should propagate exception when scraping fails for player history comparison`() {
        // Arrange
        val playerName = "Error Player"
        val year = "2023"
        `when`(playerScraper.comparePlayerStatsWithHistory(playerName, year))
            .thenThrow(RuntimeException("History comparison scraping failed"))

        // Act & Assert
        assertThrows<RuntimeException> {
            playerService.comparePlayerHistory(playerName, year)
        }
        verify(playerScraper, times(1)).comparePlayerStatsWithHistory(playerName, year)
    }
}
