package com.footballdata.football_stats_predictions.data

import com.footballdata.football_stats_predictions.model.PlayerStats
import com.footballdata.football_stats_predictions.service.StatsAnalyzer
import com.footballdata.football_stats_predictions.utils.WebDriverUtils
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import kotlin.ranges.until

@Component
class PlayerScraper(
    @field:Autowired private val statsAnalyzer: StatsAnalyzer
) {
    /**
     * Retrieves the statistical data for a specific player.
     *
     * @param playerName The name of the player to search for
     * @return PlayerStats object containing the player's statistical data
     */
    fun getPlayerData(playerName: String): PlayerStats {
        return WebDriverUtils.withSearchAndAcceptCookies(playerName) { driver ->
            // Usar un enfoque funcional para mapear encabezados con valores
            driver.findElement(By.id("player-table-statistics-head"))
                .findElements(By.tagName("tr")).first()
                .findElements(By.tagName("th"))
                .zip(driver.findElement(By.id("player-table-statistics-body"))
                    .findElements(By.tagName("tr")).last()
                    .findElements(By.tagName("td")))
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
                .let { ratings -> if (ratings.isNotEmpty()) ratings.average() else 0.0 }
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
        statsAnalyzer.compareStatsWithDiff(
            getPlayerData(playerName),
            getPlayerHistoricalRatingsByYear(playerName, year),
            "Current",
            year
        )

    private fun getPlayerHistoricalRatingsByYear(playerName: String, year: String): PlayerStats {
        return WebDriverUtils.withSearchAndSubNavigation(playerName, "Historial") { driver, wait ->
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statistics-table-summary")))

            val statsDiv = driver.findElement(By.id("statistics-table-summary"))

            // Get table headers (column names)
            val headers = statsDiv.findElement(By.tagName("thead"))
                .findElement(By.tagName("tr"))
                .findElements(By.tagName("th"))
                .map { it.text.trim() }

            // Find important indices
            val jgdosIndex = headers.indexOfFirst { it.contains("Jgdos") }.takeIf { it >= 0 } ?: 1
            val tppIndex = headers.indexOfFirst { it.contains("TpP") }.takeIf { it >= 0 } ?: headers.size

            // Find rows of the specific year
            statsDiv.findElement(By.tagName("tbody"))
                .findElements(By.tagName("tr"))
                .filter { it.findElements(By.tagName("td")).firstOrNull()?.text?.trim() == year }
                .fold(Pair(mapOf<String, Double>(), mapOf<String, Int>())) { (statsAcc, countsAcc), row ->

                    // Extract cells
                    val cells = row.findElements(By.tagName("td"))

                    // Process each cell to build statistics and counts
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
                        .groupBy({ it.first }, { it.second })

                    // Update accumulated statistics
                    val newStats = statsAcc + rowData
                        .filterKeys { !it.endsWith("_count") }
                        .mapValues { (k, values) -> (statsAcc[k] ?: 0.0) + values.sum() }

                    // Update counts
                    val newCounts = countsAcc + rowData
                        .filterKeys { it.endsWith("_count") }
                        .mapValues { (k, values) ->
                            (countsAcc[k.removeSuffix("_count")] ?: 0) + values.sum().toInt()
                        }
                        .mapKeys { it.key.removeSuffix("_count") }

                    Pair(newStats, newCounts)
                }
                .let { (stats, counts) ->
                    // Convert sums to averages where applicable
                    stats.mapValues { (header, value) ->
                        if (header in headers.subList(tppIndex, headers.size) &&
                            (counts[header] ?: 0) > 0
                        ) {
                            value / counts[header]!!
                        } else {
                            value
                        }
                    }
                }
                .let { PlayerStats(it) }
        }
    }
}