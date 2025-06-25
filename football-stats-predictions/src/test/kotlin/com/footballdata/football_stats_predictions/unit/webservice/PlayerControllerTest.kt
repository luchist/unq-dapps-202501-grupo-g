package com.footballdata.football_stats_predictions.unit.webservice

import com.footballdata.football_stats_predictions.data.FootballDataScraping
import com.footballdata.football_stats_predictions.service.PlayerService
import com.footballdata.football_stats_predictions.webservice.PlayerController
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PlayerControllerTest {

    @Mock
    private lateinit var footballDataScraping: FootballDataScraping

    @Mock
    private lateinit var playerService: PlayerService

    @InjectMocks
    private lateinit var playerController: PlayerController

    @Test
    fun `getPlayerStats should return player statistics`() {
        // Arrange
        val playerName = "Lionel Messi"
        val expectedStats = mapOf(
            "Campeonato" to 0.0,
            "Jgdos" to 35.0,
            "Mins" to 1842.0,
            "Goles" to 20.0,
            "Asist" to 6.0,
            "Amar" to 4.0,
            "Roja" to 0.0,
            "TpP" to 4.2,
            "AP%" to 84.1,
            "Aéreos" to 0.1,
            "JdelP" to 9.0,
            "Rating" to 7.93
        )
        `when`(playerService.getPlayerStats(playerName)).thenReturn(expectedStats)

        // Act
        val result = playerController.getPlayerStats(playerName)

        // Assert
        assertEquals(expectedStats, result)
        verify(playerService).getPlayerStats(playerName)
    }

    @Test
    fun `getPlayerStats should return empty map for unknown player`() {
        // Arrange
        val playerName = "Unknown Player"
        val emptyStats = emptyMap<String, Double>()
        `when`(playerService.getPlayerStats(playerName)).thenReturn(emptyStats)

        // Act
        val result = playerController.getPlayerStats(playerName)

        // Assert
        assertEquals(emptyStats, result)
        verify(playerService).getPlayerStats(playerName)
    }

    @Test
    fun `getPlayerRatingsAverage should return average rating`() {
        // Arrange
        val playerName = "Pedri"
        val expectedRating = 7.8
        `when`(playerService.getPlayerRatingsAverage(playerName)).thenReturn(expectedRating)

        // Act
        val result = playerController.getPlayerRatingsAverage(playerName)

        // Assert
        assertEquals(expectedRating, result)
        verify(playerService).getPlayerRatingsAverage(playerName)
    }

    @Test
    fun `getPlayerRatingsAverage should return zero for player with no ratings`() {
        // Arrange
        val playerName = "New Player"
        val expectedRating = 0.0
        `when`(playerService.getPlayerRatingsAverage(playerName)).thenReturn(expectedRating)

        // Act
        val result = playerController.getPlayerRatingsAverage(playerName)

        // Assert
        assertEquals(expectedRating, result)
        verify(playerService).getPlayerRatingsAverage(playerName)
    }

    @Test
    fun `getPlayerRatingsAverage should handle high rating values`() {
        // Arrange
        val playerName = "Cristiano Ronaldo"
        val expectedRating = 9.2
        `when`(playerService.getPlayerRatingsAverage(playerName)).thenReturn(expectedRating)

        // Act
        val result = playerController.getPlayerRatingsAverage(playerName)

        // Assert
        assertEquals(expectedRating, result)
        verify(playerService).getPlayerRatingsAverage(playerName)
    }

    @Test
    fun `comparePlayerHistory should return player comparison data`() {
        // Arrange
        val playerName = "Lionel Messi"
        val year = "2023"
        val expectedComparison = mapOf(
            "Actual" to mapOf(
                "Campeonato" to "0.0 (0.00)",
                "Jgdos" to "35.0 (35.00)",
                "Mins" to "1842.0 (1469.00)",
                "Goles" to "20.0 (19.00)",
                "Asist" to "6.0 (4.00)",
                "Amar" to "4.0 (4.00)",
                "Roja" to "0.0 (0.00)",
                "TpP" to "4.2 (0.90)",
                "AP%" to "84.1 (6.10)",
                "Aéreos" to "0.1 (-0.10)",
                "JdelP" to "9.0 (8.00)",
                "Rating" to "7.93 (0.85)"
            ),
            year to mapOf(
                "Campeonato" to "0.0 (-0.00)",
                "Jgdos" to "0.0 (-35.00)",
                "Mins" to "373.0 (-1469.00)",
                "Goles" to "1.0 (-19.00)",
                "Asist" to "2.0 (-4.00)",
                "Amar" to "0.0 (-4.00)",
                "Roja" to "0.0 (-0.00)",
                "TpP" to "3.3 (-0.90)",
                "AP%" to "78.0 (-6.10)",
                "Aéreos" to "0.2 (0.10)",
                "JdelP" to "1.0 (-8.00)",
                "Rating" to "7.08 (-0.85)"
            )
        )
        `when`(playerService.comparePlayerHistory(playerName, year)).thenReturn(expectedComparison)

        // Act
        val result = playerController.comparePlayerHistory(playerName, year)

        // Assert
        assertEquals(expectedComparison, result)
        verify(playerService).comparePlayerHistory(playerName, year)
    }

    @Test
    fun `comparePlayerHistory should handle empty comparison for new player`() {
        // Arrange
        val playerName = "Young Player"
        val year = "2023"
        val emptyComparison = emptyMap<String, Map<String, String>>()
        `when`(playerService.comparePlayerHistory(playerName, year)).thenReturn(emptyComparison)

        // Act
        val result = playerController.comparePlayerHistory(playerName, year)

        // Assert
        assertEquals(emptyComparison, result)
        verify(playerService).comparePlayerHistory(playerName, year)
    }

    @Test
    fun `comparePlayerHistory should handle different year formats`() {
        // Arrange
        val playerName = "Gavi"
        val year = "2022"
        val expectedComparison = mapOf(
            "goals" to mapOf(
                "2024" to "5",
                "2022" to "2",
                "difference" to "+3"
            ),
            "pass_accuracy" to mapOf(
                "2024" to "91.2",
                "2022" to "87.8",
                "difference" to "+3.4"
            )
        )
        `when`(playerService.comparePlayerHistory(playerName, year)).thenReturn(expectedComparison)

        // Act
        val result = playerController.comparePlayerHistory(playerName, year)

        // Assert
        assertEquals(expectedComparison, result)
        verify(playerService).comparePlayerHistory(playerName, year)
    }

    @Test
    fun `comparePlayerHistory should handle identical performance years`() {
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
        `when`(playerService.comparePlayerHistory(playerName, year)).thenReturn(identicalComparison)

        // Act
        val result = playerController.comparePlayerHistory(playerName, year)

        // Assert
        assertEquals(identicalComparison, result)
        verify(playerService).comparePlayerHistory(playerName, year)
    }
}
