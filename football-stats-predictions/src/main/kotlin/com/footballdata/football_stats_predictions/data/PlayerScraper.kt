package com.footballdata.football_stats_predictions.data

import com.footballdata.football_stats_predictions.model.PlayerStats
import com.footballdata.football_stats_predictions.model.PlayerStatsBuilder
import com.footballdata.football_stats_predictions.service.StatsAnalyzerService
import com.footballdata.football_stats_predictions.utils.WebDriverUtils
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

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
            val headers = extractPlayerStatHeaders(driver)
            val values = extractPlayerStatValues(driver)
            val statsMap = zipHeadersWithValues(headers, values)
            PlayerStatsBuilder()
                .withPlayerName(playerName)
                .withData(statsMap)
                .build()
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
            waitForMatchStatisticsTable(wait)
            val ratings = extractPlayerRatings(driver)
            statsAnalyzerService.calculateAverageWithRounding(ratings)
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

            // Processing data from the specific year rows
            val (stats, counts) = processYearData(statsDiv, year, headers, jgdosIndex, tppIndex)

            // Convert sums to averages where applicable
            val finalStats = statsAnalyzerService.calculateStatsAverages(stats, counts, headers, tppIndex)

            PlayerStatsBuilder()
                .withPlayerName(playerName)
                .withData(finalStats)
                .build()
        }
    }

    private fun extractTableHeaders(statsDiv: WebElement): List<String> {
        return statsDiv.findElement(By.tagName("thead"))
            .findElement(By.tagName("tr"))
            .findElements(By.tagName("th"))
            .map { it.text.trim() }
    }

    /**
     * Extracts the header elements from the player statistics table.
     */
    private fun extractPlayerStatHeaders(driver: WebDriver): List<WebElement> {
        return driver.findElement(By.id("player-table-statistics-head"))
            .findElements(By.tagName("tr")).first()
            .findElements(By.tagName("th"))
            .drop(1)
    }

    /**
     * Extracts the value elements from the player statistics table.
     */
    private fun extractPlayerStatValues(driver: WebDriver): List<WebElement> {
        return driver.findElement(By.id("player-table-statistics-body"))
            .findElements(By.tagName("tr")).last()
            .findElements(By.tagName("td"))
            .drop(1)
    }

    /**
     * Combines header elements with their corresponding value elements and converts to a map.
     */
    private fun zipHeadersWithValues(headers: List<WebElement>, values: List<WebElement>): Map<String, Double> {
        return headers.zip(values)
            .associate { (header, cell) ->
                header.text to (cell.text.toDoubleOrNull() ?: 0.0)
            }
    }

    /**
     * Waits for the match statistics table to be visible.
     */
    private fun waitForMatchStatisticsTable(wait: WebDriverWait) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statistics-table-summary-matches")))
    }

    /**
     * Extracts player ratings from the match statistics table.
     */
    private fun extractPlayerRatings(driver: WebDriver): List<Double> {
        return driver.findElement(By.id("statistics-table-summary-matches"))
            .findElement(By.id("player-table-statistics-body"))
            .findElements(By.cssSelector("td.rating"))
            .mapNotNull { it.text.trim().toDoubleOrNull() }
            .take(10)
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