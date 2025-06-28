package com.footballdata.football_stats_predictions.data

import com.footballdata.football_stats_predictions.model.TeamStats
import com.footballdata.football_stats_predictions.service.StatsAnalyzer
import com.footballdata.football_stats_predictions.utils.WebDriverUtils
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TeamScraper(
    @field:Autowired private val statsAnalyzer: StatsAnalyzer
) {

    /**
     * Retrieves the basic statistical data for a specific team.
     * Scrapes information from the team's statistics page including goals, cards, and other metrics.
     *
     * @param teamName The name of the team to search for
     * @return TeamStats object containing the team's statistical data
     */
    fun getTeamData(teamName: String): TeamStats {
        return WebDriverUtils.withDriver(teamName) { driver ->
            // Find the team statistics table body and header
            val tableBody = driver.findElement(By.id("top-team-stats-summary-content"))
            val tableHeader = driver.findElement(By.cssSelector("#top-team-stats-summary-grid thead"))

            // Find the last row of the statistics table, and the first row of the header
            val totalRow = (tableBody.findElements(By.tagName("tr"))).last()
            val namesRow = (tableHeader.findElements(By.tagName("tr"))).first()

            // Maps the names of the columns with the values of the last row using totalRow and namesRow
            // Note: the first cell contains the "Campeonato" and "Total/Promedio" headers, so we skip it
            // Use zip to map headers with values and transform functionally
            namesRow.findElements(By.tagName("th"))
                .drop(1)
                .zip(totalRow.findElements(By.tagName("td")).drop(1))
                .flatMap { (header, cell) ->
                    if (header.text == "Disciplina") {
                        listOf(
                            "Yellow Cards" to (cell.findElement(By.cssSelector(".yellow-card-box")).text.toDoubleOrNull() ?: 0.0),
                            "Red Cards" to (cell.findElement(By.cssSelector(".red-card-box")).text.toDoubleOrNull() ?: 0.0)
                        )
                    } else {
                        listOf(header.text to (cell.text.toDoubleOrNull() ?: 0.0))
                    }
                }
                .toMap()
                .let { TeamStats(it) }
        }
    }

    /**
     * Compares the statistical data between two teams and calculates the differences.
     * Provides a formatted comparison with differences highlighted.
     *
     * @param team1 The name of the first team to compare
     * @param team2 The name of the second team to compare
     * @return A map containing two submaps, each with formatted team statistics and their differences
     */
    fun compareTeamStatsWithDiff(
        team1: String,
        team2: String
    ): Map<String, Map<String, String>> =
        statsAnalyzer.compareStatsWithDiff(
            getTeamData(team1),
            getTeamData(team2),
            team1,
            team2
        )

    /**
     * Retrieves advanced statistics for a team by combining basic stats with calculated metrics.
     * Includes match results (wins/draws/losses) and derived metrics like shot effectiveness.
     *
     * @param teamName The name of the team to analyze
     * @return TeamStats object containing both basic and advanced statistical data
     */
    fun getTeamAdvancedStatistics(teamName: String): TeamStats =
        getTeamData(teamName)
            .let { stats ->
                val advancedStats = statsAnalyzer.getTeamGoalsAndShotEffectiveness(stats)
                val results = scrapeMatchResults(teamName)

                TeamStats(stats.data + advancedStats.data + results.data)
            }

    private fun scrapeMatchResults(teamName: String): TeamStats {
        return WebDriverUtils.withDriver(teamName) { driver ->
            val wait = WebDriverUtils.clickOnSubNavigationLink(driver, "Encuentros")

            // Wait for the fixtures wrapper to appear
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("team-fixture-wrapper")))

            // Get all items of results
            driver.findElement(By.id("team-fixture-wrapper"))
                  .findElements(By.cssSelector("a[class^=' box ']"))
                // Convert each item into its result type
                .map { box ->
                    when {
                        box.getAttribute("class")?.contains("box w") == true -> ResultType.WIN
                        box.getAttribute("class")?.contains("box d") == true -> ResultType.DRAW
                        box.getAttribute("class")?.contains("box l") == true -> ResultType.LOSS
                        else -> ResultType.OTHER
                    }
                }
                // Group and count results
                .groupingBy { it }
                .eachCount()
                // Convert a count map to double values
                .let { countMap ->
                    mapOf(
                        "Wins" to (countMap[ResultType.WIN] ?: 0).toDouble(),
                        "Draws" to (countMap[ResultType.DRAW] ?: 0).toDouble(),
                        "Losses" to (countMap[ResultType.LOSS] ?: 0).toDouble()
                    )
                }
                // Create the TeamStats object with the final map
                .let { TeamStats(it) }
        }
    }
    private enum class ResultType { WIN, DRAW, LOSS, OTHER }
}