package com.footballdata.football_stats_predictions.data

import com.footballdata.football_stats_predictions.model.PlayerStats
import com.footballdata.football_stats_predictions.service.StatsAnalyzerService
import com.footballdata.football_stats_predictions.utils.WebDriverUtils
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import kotlin.math.roundToInt

@Component
class PlayerScraper(
    @field:Autowired private val statsAnalyzerService: StatsAnalyzerService
) {
    /**
     * Retrieves the statistical data for a specific player.
     *
     * @param playerName The name of the player to search for
     * @return PlayerStats object containing the player's statistical data
     */
    fun getPlayerData(playerName: String): PlayerStats {
        return WebDriverUtils.withSearchAndAcceptCookies(playerName) { driver ->
            // Use a functional approach to map headers with values
            driver.findElement(By.id("player-table-statistics-head"))
                .findElements(By.tagName("tr")).first()
                .findElements(By.tagName("th"))
                .drop(1)
                .zip(
                    driver.findElement(By.id("player-table-statistics-body"))
                        .findElements(By.tagName("tr")).last()
                        .findElements(By.tagName("td"))
                        .drop(1)
                )
                .associate { (header, cell) ->
                    header.text to (cell.text.toDoubleOrNull() ?: 0.0)
                }
                .let { PlayerStats(it) }
        }
    }

    /**
     * Calculates the average rating of a player based on their last matches.
     * Takes up to 10 most recent matches into consideration.
     *
     * @param playerName The name of the player to search for
     * @return The average rating of the player, or 0.0 if no ratings are found
     */
    fun getPlayerRatingsAverage(playerName: String): Double {
        return WebDriverUtils.withSearchAndSubNavigation(playerName, "Estadísticas del Partido") { driver, wait ->
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statistics-table-summary-matches")))

            driver.findElement(By.id("statistics-table-summary-matches"))
                .findElement(By.id("player-table-statistics-body"))
                .findElements(By.cssSelector("td.rating"))
                .mapNotNull { it.text.trim().toDoubleOrNull() }
                .take(10)
                .let { ratings ->
                    if (ratings.isNotEmpty())
                        (ratings.average() * 100).roundToInt() / 100.0
                    else 0.0
                }
        }
    }

    /**
     * Compares a player's current statistics with their historical statistics from a specific year.
     * Provides a comparison with differences between current and historical values.
     *
     * @param playerName The name of the player to search for
     * @param year The year to compare with (e.g., "2023")
     * @return A map containing two maps with formatted statistics and their differences
     */
    fun comparePlayerStatsWithHistory(playerName: String, year: String): Map<String, Map<String, String>> =
        statsAnalyzerService.compareStatsWithDiff(
            getPlayerData(playerName),
            getPlayerHistoricalRatingsByYear(playerName, year),
            "Current",
            year
        )

    private fun getPlayerHistoricalRatingsByYear(playerName: String, year: String): PlayerStats {
        return WebDriverUtils.withSearchAndSubNavigation(playerName, "Historial") { driver, wait ->
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statistics-table-summary")))

            val statsDiv = driver.findElement(By.id("statistics-table-summary"))

            // Get important headings and indexes
            val headers = extractTableHeaders(statsDiv)
            val (jgdosIndex, tppIndex) = statsAnalyzerService.findColumnIndices(headers, "Jgdos", "TpP")

            // Procesar datos de las filas del año específico
            val (stats, counts) = processYearData(statsDiv, year, headers, jgdosIndex, tppIndex)

            // Convertir sumas a promedios donde corresponda
            val finalStats = statsAnalyzerService.calculateStatsAverages(stats, counts, headers, tppIndex)

            PlayerStats(finalStats)
        }
    }

    private fun extractTableHeaders(statsDiv: WebElement): List<String> {
        return statsDiv.findElement(By.tagName("thead"))
            .findElement(By.tagName("tr"))
            .findElements(By.tagName("th"))
            .map { it.text.trim() }
    }

    private fun processYearData(
        statsDiv: WebElement,
        year: String,
        headers: List<String>,
        jgdosIndex: Int,
        tppIndex: Int
    ): Pair<Map<String, Double>, Map<String, Int>> {
        return statsDiv.findElement(By.tagName("tbody"))
            .findElements(By.tagName("tr"))
            .filter { it.findElements(By.tagName("td")).firstOrNull()?.text?.trim() == year }
            .fold(Pair(mapOf(), mapOf())) { (statsAcc, countsAcc), row ->
                processRowData(row, headers, jgdosIndex, tppIndex, statsAcc, countsAcc)
            }
    }

    private fun processRowData(
        row: WebElement,
        headers: List<String>,
        jgdosIndex: Int,
        tppIndex: Int,
        statsAcc: Map<String, Double>,
        countsAcc: Map<String, Int>
    ): Pair<Map<String, Double>, Map<String, Int>> {
        val cells = row.findElements(By.tagName("td"))

        val rowData = (jgdosIndex until cells.size.coerceAtMost(headers.size))
            .flatMap { i ->
                val header = headers[i]
                val valueText = cells[i].text.trim()
                val value = valueText.toDoubleOrNull() ?: 0.0
                val isValidContribution = valueText.isNotEmpty() && valueText != "-" && i >= tppIndex

                listOf(
                    header to value,
                    "${header}_count" to (if (isValidContribution) 1.0 else 0.0)
                )
            }

        return statsAnalyzerService.processStatsData(rowData, statsAcc, countsAcc)
    }
}