package com.footballdata.football_stats_predictions.unit.service

import com.footballdata.football_stats_predictions.data.PlayerScraper
import com.footballdata.football_stats_predictions.model.Comparison
import com.footballdata.football_stats_predictions.model.ComparisonType
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
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
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

    @InjectMocks
    private lateinit var persistenceHelper: PersistenceHelper

    @BeforeEach
    fun setup() {
        playerService = PlayerService(
            playerScraper,
            playerRepository,
            playerStatsRepository,
            comparisonRepository,
            persistenceHelper
        )
    }

    @Test
    fun `should return player statistics from scraping service`() {
        // Arrange
        val playerName = "Lionel Messi"
        val expectedStats = PlayerStatsBuilder()
            .withPlayerName(playerName)
            .withData(
                mapOf(
                    "goals" to 15.0,
                    "assists" to 8.0,
                    "shots_per_game" to 4.2,
                    "pass_accuracy" to 89.5,
                    "dribbles_completed" to 3.8,
                    "minutes_played" to 2340.0,
                    "key_passes" to 2.1,
                    "successful_tackles" to 0.8
                )
            )
            .build()

        // Mock que no hay datos en caché
        `when`(playerStatsRepository.findByPlayerName(playerName)).thenReturn(null)
        // Mock del scraper
        `when`(playerScraper.getPlayerData(playerName)).thenReturn(expectedStats)
        // Mock del repositorio al guardar
        `when`(playerStatsRepository.save(any())).thenReturn(expectedStats)

        // Act
        val result = playerService.getPlayerStats(playerName)

        // Assert
        assert(result == expectedStats)

        // Verificar todas las dependencias
        verify(playerStatsRepository, times(1)).findByPlayerName(playerName)
        verify(playerScraper, times(1)).getPlayerData(playerName)
        verify(playerStatsRepository, times(1)).save(any())
    }

    @Test
    fun `should return empty map when no player statistics available`() {
        // Arrange
        val playerName = "Unknown Player"
        val emptyStats = PlayerStatsBuilder()
            .withPlayerName(playerName)
            .withData(emptyMap())
            .build()

        // Mock que no hay datos en caché
        `when`(playerStatsRepository.findByPlayerName(playerName)).thenReturn(null)

        // Mock del scraper que devuelve estadísticas vacías
        `when`(playerScraper.getPlayerData(playerName)).thenReturn(emptyStats)

        // Mock del repositorio al guardar
        `when`(playerStatsRepository.save(any())).thenReturn(emptyStats)

        // Act
        val result = playerService.getPlayerStats(playerName)

        // Assert
        assert(result.isEmpty())

        // Verificar todas las dependencias
        verify(playerStatsRepository, times(1)).findByPlayerName(playerName)
        verify(playerScraper, times(1)).getPlayerData(playerName)
        verify(playerStatsRepository, times(1)).save(any())
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
            "2024" to mapOf(
                "Jgdos" to "32 (5)",
                "Mins" to "2430 (640)",
                "Goles" to "5 (3)",
                "Asist" to "4 (-2)",
                "Rating" to "7.42 (0.38)"
            ),
            "2023" to mapOf(
                "Jgdos" to "27 (-5)",
                "Mins" to "1790 (-640)",
                "Goles" to "2 (-3)",
                "Asist" to "6 (2)",
                "Rating" to "7.04 (-0.38)"
            )
        )

        val comparisonMock = Comparison(
            id = 0L,
            comparisonType = ComparisonType.PLAYER,
            entity1Name = playerName,
            entity2Name = year,
            comparisonData = expectedComparison
        )

        // Mock que no hay datos en caché
        `when`(
            comparisonRepository.findByComparisonTypeAndEntity1NameAndEntity2Name(
                ComparisonType.PLAYER, playerName, year
            )
        ).thenReturn(null)

        // Mock del scraper
        `when`(playerScraper.comparePlayerStatsWithHistory(playerName, year)).thenReturn(expectedComparison)

        // Mock del repositorio al guardar
        `when`(comparisonRepository.save(any())).thenReturn(comparisonMock)

        // Act
        val result = playerService.comparePlayerHistory(playerName, year)

        // Assert
        assert(result == expectedComparison)

        // Verificar todas las dependencias
        verify(comparisonRepository, times(1)).findByComparisonTypeAndEntity1NameAndEntity2Name(
            ComparisonType.PLAYER, playerName, year
        )
        verify(playerScraper, times(1)).comparePlayerStatsWithHistory(playerName, year)
        verify(comparisonRepository, times(1)).save(any())
    }

    @Test
    fun `should handle empty player history comparison for new player`() {
        // Arrange
        val playerName = "Young Player"
        val year = "2023"
        val emptyComparison = emptyMap<String, Map<String, String>>()
        val emptyComparisonEntity = Comparison(
            id = 0L,
            comparisonType = ComparisonType.PLAYER,
            entity1Name = playerName,
            entity2Name = year,
            comparisonData = emptyComparison
        )

        // Mock que no hay datos en caché
        `when`(
            comparisonRepository.findByComparisonTypeAndEntity1NameAndEntity2Name(
                ComparisonType.PLAYER, playerName, year
            )
        ).thenReturn(null)

        // Mock del scraper
        `when`(playerScraper.comparePlayerStatsWithHistory(playerName, year)).thenReturn(emptyComparison)

        // Mock del repositorio al guardar
        `when`(comparisonRepository.save(any())).thenReturn(emptyComparisonEntity)

        // Act
        val result = playerService.comparePlayerHistory(playerName, year)

        // Assert
        assert(result.isEmpty())

        // Verificar todas las dependencias
        verify(comparisonRepository, times(1)).findByComparisonTypeAndEntity1NameAndEntity2Name(
            ComparisonType.PLAYER, playerName, year
        )
        verify(playerScraper, times(1)).comparePlayerStatsWithHistory(playerName, year)
        verify(comparisonRepository, times(1)).save(any())
    }

    @Test
    fun `should handle identical performance across years`() {
        // Arrange
        val playerName = "Consistent Player"
        val year = "2023"
        val identicalComparison = mapOf(
            "2024" to mapOf(
                "Goles" to "10 (0)",
                "Asist" to "5 (0)",
                "Rating" to "7.5 (0.0)"
            ),
            "2023" to mapOf(
                "Goles" to "10 (0)",
                "Asist" to "5 (0)",
                "Rating" to "7.5 (0.0)"
            )
        )
        val comparisonEntity = Comparison(
            id = 0L,
            comparisonType = ComparisonType.PLAYER,
            entity1Name = playerName,
            entity2Name = year,
            comparisonData = identicalComparison
        )

        // Mock que no hay datos en caché
        `when`(
            comparisonRepository.findByComparisonTypeAndEntity1NameAndEntity2Name(
                ComparisonType.PLAYER, playerName, year
            )
        ).thenReturn(null)

        // Mock del scraper
        `when`(playerScraper.comparePlayerStatsWithHistory(playerName, year)).thenReturn(identicalComparison)

        // Mock del repositorio al guardar
        `when`(comparisonRepository.save(any())).thenReturn(comparisonEntity)

        // Act
        val result = playerService.comparePlayerHistory(playerName, year)

        // Assert
        assert(result == identicalComparison)

        // Verificar todas las dependencias
        verify(comparisonRepository, times(1)).findByComparisonTypeAndEntity1NameAndEntity2Name(
            ComparisonType.PLAYER, playerName, year
        )
        verify(playerScraper, times(1)).comparePlayerStatsWithHistory(playerName, year)
        verify(comparisonRepository, times(1)).save(any())
    }

    @Test
    fun `should handle different year formats in comparison`() {
        // Arrange
        val playerName = "Gavi"
        val year = "22-23"
        val formattedComparison = mapOf(
            "2024" to mapOf(
                "Jgdos" to "32 (5)",
                "Mins" to "2430 (640)",
                "Goles" to "5 (3)"
            ),
            "22-23" to mapOf(
                "Jgdos" to "27 (-5)",
                "Mins" to "1790 (-640)",
                "Goles" to "2 (-3)"
            )
        )
        val comparisonEntity = Comparison(
            id = 0L,
            comparisonType = ComparisonType.PLAYER,
            entity1Name = playerName,
            entity2Name = year,
            comparisonData = formattedComparison
        )

        // Mock que no hay datos en caché
        `when`(
            comparisonRepository.findByComparisonTypeAndEntity1NameAndEntity2Name(
                ComparisonType.PLAYER, playerName, year
            )
        ).thenReturn(null)

        // Mock del scraper
        `when`(playerScraper.comparePlayerStatsWithHistory(playerName, year)).thenReturn(formattedComparison)

        // Mock del repositorio al guardar
        `when`(comparisonRepository.save(any())).thenReturn(comparisonEntity)

        // Act
        val result = playerService.comparePlayerHistory(playerName, year)

        // Assert
        assert(result == formattedComparison)

        // Verificar todas las dependencias
        verify(comparisonRepository, times(1)).findByComparisonTypeAndEntity1NameAndEntity2Name(
            ComparisonType.PLAYER, playerName, year
        )
        verify(playerScraper, times(1)).comparePlayerStatsWithHistory(playerName, year)
        verify(comparisonRepository, times(1)).save(any())
    }

    @Test
    fun `should handle negative performance differences`() {
        // Arrange
        val playerName = "Declining Player"
        val year = "2023"
        val negativeComparison = mapOf(
            "2024" to mapOf(
                "Goles" to "5 (-5)",
                "Asist" to "3 (-2)",
                "Rating" to "6.8 (-0.7)"
            ),
            "2023" to mapOf(
                "Goles" to "10 (5)",
                "Asist" to "5 (2)",
                "Rating" to "7.5 (0.7)"
            )
        )
        val comparisonEntity = Comparison(
            id = 0L,
            comparisonType = ComparisonType.PLAYER,
            entity1Name = playerName,
            entity2Name = year,
            comparisonData = negativeComparison
        )

        // Mock que no hay datos en caché
        `when`(
            comparisonRepository.findByComparisonTypeAndEntity1NameAndEntity2Name(
                ComparisonType.PLAYER, playerName, year
            )
        ).thenReturn(null)

        // Mock del scraper
        `when`(playerScraper.comparePlayerStatsWithHistory(playerName, year)).thenReturn(negativeComparison)

        // Mock del repositorio al guardar
        `when`(comparisonRepository.save(any())).thenReturn(comparisonEntity)

        // Act
        val result = playerService.comparePlayerHistory(playerName, year)

        // Assert
        assert(result == negativeComparison)

        // Verificar todas las dependencias
        verify(comparisonRepository, times(1)).findByComparisonTypeAndEntity1NameAndEntity2Name(
            ComparisonType.PLAYER, playerName, year
        )
        verify(playerScraper, times(1)).comparePlayerStatsWithHistory(playerName, year)
        verify(comparisonRepository, times(1)).save(any())
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
