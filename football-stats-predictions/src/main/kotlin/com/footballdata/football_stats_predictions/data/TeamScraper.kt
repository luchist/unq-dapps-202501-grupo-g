package com.footballdata.football_stats_predictions.data

import com.footballdata.football_stats_predictions.model.ResultType
import com.footballdata.football_stats_predictions.model.TeamStats
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
class TeamScraper(
    @field:Autowired private val statsAnalyzerService: StatsAnalyzerService
) {
    /**
     * Retrieves the basic statistical data for a specific team.
     * Scrapes information from the team's statistics page including goals, cards, and other metrics.
     *
     * @param teamName The name of the team to search for
     * @return TeamStats object containing the team's statistical data
     */
    fun getTeamData(teamName: String): TeamStats {
        return WebDriverUtils.withSearchAndAcceptCookies(teamName) { driver ->
            // Extract headers and values
            val headerElements = extractTableHeaders(driver)
            val valueElements = extractTotalRowValues(driver)

            // Combine headers with values and build statistics
            val headerValuePairs = zipHeadersWithValues(headerElements, valueElements)
            val statsMap = statsAnalyzerService.buildTeamStatsMap(headerValuePairs)

            TeamStats(statsMap)
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
        statsAnalyzerService.compareStatsWithDiff(
            getTeamData(team1),
            getTeamData(team2),
            team1,
            team2
        )

    /**
     * Predicts the outcome probabilities of a match between two teams.
     * Retrieves statistics for both teams and processes them through the analyzer
     * to calculate probabilities for home win, draw, and away win.
     *
     * @param team1 Name of the home team
     * @param team2 Name of the away team
     * @return A map with probabilities (in percentage) for each possible outcome
     */
    fun predictMatchProbabilities(
        team1: String,
        team2: String
    ): Map<String, Double> =
        statsAnalyzerService.predictMatch(
            getTeamAdvancedStatistics(team1),
            getTeamAdvancedStatistics(team2)
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
                val advancedStats = statsAnalyzerService.getTeamGoalsAndShotEffectiveness(stats)
                val results = scrapeMatchResults(teamName)

                TeamStats(stats.data + advancedStats.data + results.data)
            }

    /**
     * Scrapes the match results for a specific team from their fixture page.
     * Navigates to the team's matches section, extracts result elements,
     * classifies them (win/draw/loss), and converts the counts into statistics.
     *
     * @param teamName The name of the team to search for
     * @return TeamStats object containing the team's match result statistics (wins, draws, losses)
     */
    private fun scrapeMatchResults(teamName: String): TeamStats {
        return WebDriverUtils.withSearchAndSubNavigation(teamName, "Encuentros") { driver, wait ->
            // Wait and get the items
            val matchBoxes = waitForAndExtractMatchBoxes(driver, wait)

            // Ranking and counting results
            val resultCounts = countMatchResults(matchBoxes)

            // Convert counts to statistics
            statsAnalyzerService.convertMatchResultsToStats(resultCounts)
        }
    }

    /**
     * Wait for it to load the match container and extract the result items
     */
    private fun waitForAndExtractMatchBoxes(driver: WebDriver, wait: WebDriverWait): List<WebElement> {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("team-fixture-wrapper")))
        return driver.findElement(By.id("team-fixture-wrapper"))
            .findElements(By.cssSelector("a[class^=' box ']"))
    }

    /**
     * Sort each match result and count occurrences by type
     */
    private fun countMatchResults(matchBoxes: List<WebElement>): Map<ResultType, Int> {
        return matchBoxes
            .map { box ->
                statsAnalyzerService.classifyMatchResult(box.getAttribute("class"))
            }
            .groupingBy { it }
            .eachCount()
    }

    /**
     * Extracts the header elements from the team statistics table
     */
    private fun extractTableHeaders(driver: WebDriver): List<WebElement> {
        return driver.findElement(By.id("top-team-stats-summary-grid"))
            .findElement(By.tagName("thead"))
            .findElements(By.tagName("tr")).first()
            .findElements(By.tagName("th"))
            .drop(1)  // Skip the first header cell which contains metadata
    }

    /**
     * Extracts the values from the total row in the team statistics table
     */
    private fun extractTotalRowValues(driver: WebDriver): List<WebElement> {
        return driver.findElement(By.id("top-team-stats-summary-content"))
            .findElements(By.tagName("tr")).last()
            .findElements(By.tagName("td"))
            .drop(1)  // Skip the first cell which contains "Total/Promedio"
    }

    /**
     * Combines header elements with their corresponding value elements
     */
    private fun zipHeadersWithValues(
        headers: List<WebElement>,
        values: List<WebElement>
    ): List<Pair<WebElement, WebElement>> {
        return headers.zip(values)
    }
}