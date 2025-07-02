package com.footballdata.football_stats_predictions.service

import com.footballdata.football_stats_predictions.model.ResultType
import com.footballdata.football_stats_predictions.model.Stats
import com.footballdata.football_stats_predictions.model.TeamStats
import com.footballdata.football_stats_predictions.model.TeamStatsBuilder
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

@Service
@Transactional
class StatsAnalyzerService() {
    /**
     * Compares two sets of statistics and calculates the differences between them.
     *
     * @param stats1 First set of statistics to compare
     * @param stats2 Second set of statistics to compare
     * @param key1 Label for the first set of statistics
     * @param key2 Label for the second set of statistics
     * @return A map containing two submaps, each with formatted statistics and their differences
     */
    fun compareStatsWithDiff(
        stats1: Stats,
        stats2: Stats,
        key1: String,
        key2: String
    ): Map<String, Map<String, String>> =
        (stats1.keys + stats2.keys).let { allKeys ->
            mapOf(
                key1 to allKeys.associateWith { key ->
                    val diff = stats1[key] - stats2[key]
                    "${stats1[key]} (${diff.formatTwoDecimals()})"
                },
                key2 to allKeys.associateWith { key ->
                    val diff = stats1[key] - stats2[key]
                    "${stats2[key]} (${(-diff).formatTwoDecimals()})"
                }
            )
        }

    /**
     * Calculates advanced goal and shot effectiveness metrics for a team using functional approach.
     * Performs calculations immutably and rounds results to two decimal places.
     *
     * @param stats The base team statistics to analyze
     * @return TeamStats object containing derived metrics: Goals per game and Shot Effectiveness (rounded to two decimals)
     */
    fun getTeamGoalsAndShotEffectiveness(stats: TeamStats): TeamStats =
        stats.run {
            val goalsPerGame = (this["Goles"] / (this["Apps"].takeIf { it != 0.0 } ?: 1.0))
                .toRounded()

            val shotEffectiveness = (if (this["Goles"] > 0.0)
                this["Tiros pp"] / goalsPerGame
            else 0.0)
                .toRounded()

            TeamStatsBuilder()
                .withData(mapOf(
                    "Goals per game" to goalsPerGame,
                    "Shot Effectiveness" to shotEffectiveness
                ))
                .build()
        }

    /**
     * Predicts the probabilities of match outcomes between two teams.
     *
     * @param localStats Statistics of the home team
     * @param visitingStats Statistics of the away team
     * @return Map containing probability percentages for each possible outcome
     */
    fun predictMatch(localStats: TeamStats, visitingStats: TeamStats): Map<String, Double> {
        val weights = getWeights(localStats)
        val scoreLocal = calcScore(localStats, weights)
        val scoreVisiting = calcScore(visitingStats, weights)
        val scoreDraw = calculateDrawScore(scoreLocal, scoreVisiting)

        return calculateOutcomeProbabilities(scoreLocal, scoreDraw, scoreVisiting)
    }

    /**
     * Calculates average values for accumulated statistics based on their counters.
     * Divides accumulated values by their respective counters to get average values.
     *
     * @param stats Map with accumulated statistics
     * @param counts Map with counters for each statistic
     * @param headers List of column headers
     * @param startIndex Index from which to apply average calculation
     * @return Map with processed statistics, calculating averages where appropriate
     */
    fun calculateStatsAverages(
        stats: Map<String, Double>,
        counts: Map<String, Int>,
        headers: List<String>,
        startIndex: Int
    ): Map<String, Double> {
        return stats.mapValues { (header, value) ->
            if (header in headers.subList(startIndex, headers.size) &&
                (counts[header] ?: 0) > 0
            ) {
                ((value / counts[header]!!) * 100).roundToInt() / 100.0
            } else {
                value
            }
        }
    }

    /**
     * Processes raw statistical data and incorporates it into existing maps.
     *
     * @param rawData List of pairs with raw statistical data
     * @param currentStats Current map of accumulated statistics
     * @param currentCounts Current map of counters
     * @return Pair of updated maps (statistics and counters)
     */
    fun processStatsData(
        rawData: List<Pair<String, Double>>,
        currentStats: Map<String, Double>,
        currentCounts: Map<String, Int>
    ): Pair<Map<String, Double>, Map<String, Int>> {
        val rowData = groupDataByKey(rawData)
        val newStats = updateStatsMap(rowData, currentStats)
        val newCounts = updateCountsMap(rowData, currentCounts)

        return Pair(newStats, newCounts)
    }

    /**
     * Finds indices of specific columns in a list of headers.
     * Searches for headers containing the specified names and returns their positions.
     *
     * @param headers List of column headers
     * @param nameStart Name to identify the starting column
     * @param nameEnd Name to identify the ending column
     * @return Pair of indices representing start and end positions
     */
    fun findColumnIndices(
        headers: List<String>,
        nameStart: String,
        nameEnd: String
    ): Pair<Int, Int> {
        val startIndex = headers.indexOfFirst { it.contains(nameStart) }.takeIf { it >= 0 } ?: 1
        val endIndex = headers.indexOfFirst { it.contains(nameEnd) }.takeIf { it >= 0 } ?: headers.size
        return Pair(startIndex, endIndex)
    }

    /**
     * Converts a map of match result counts into a TeamStats object.
     * Transforms the counts of wins, draws, and losses into double values
     * that can be processed alongside other team statistics.
     *
     * @param resultCounts Map containing counts for each result type (WIN, DRAW, LOSS)
     * @return TeamStats object with numeric representations of match results
     */
    fun convertMatchResultsToStats(resultCounts: Map<ResultType, Int>): TeamStats {
        return TeamStatsBuilder()
            .withData(mapOf(
                "Wins" to (resultCounts[ResultType.WIN] ?: 0).toDouble(),
                "Draws" to (resultCounts[ResultType.DRAW] ?: 0).toDouble(),
                "Losses" to (resultCounts[ResultType.LOSS] ?: 0).toDouble()
            ))
            .build()
    }

    /**
     * Classifies a match result based on the CSS class of its container element.
     * Analyzes the class name to determine if the result represents a win, draw, or loss.
     *
     * @param className The CSS class string from the match result element
     * @return ResultType enum value (WIN, DRAW, LOSS, or OTHER if unrecognized)
     */
    fun classifyMatchResult(className: String?): ResultType {
        return when {
            className?.contains("box w") == true -> ResultType.WIN
            className?.contains("box d") == true -> ResultType.DRAW
            className?.contains("box l") == true -> ResultType.LOSS
            else -> ResultType.OTHER
        }
    }

    /**
     * Processes special cells such as discipline cells that contain multiple values
     *
     * @param header Name of column header
     * @param cell Element containing the data
     * @return List of pairs with name and numeric value
     */
    fun processSpecialTeamStatsCell(header: String, cell: WebElement): List<Pair<String, Double>> {
        return if (header == "Disciplina") {
            listOf(
                "Yellow Cards" to (cell.findElement(By.cssSelector(".yellow-card-box")).text.toDoubleOrNull() ?: 0.0),
                "Red Cards" to (cell.findElement(By.cssSelector(".red-card-box")).text.toDoubleOrNull() ?: 0.0)
            )
        } else {
            listOf(header to (cell.text.toDoubleOrNull() ?: 0.0))
        }
    }

    /**
     * Builds a statistics map from header and cell pairs
     *
     * @param headerCellPairs List of header and cell pairs
     * @return Map with processed statistics
     */
    fun buildTeamStatsMap(headerCellPairs: List<Pair<WebElement, WebElement>>): Map<String, Double> {
        return headerCellPairs
            .flatMap { (header, cell) ->
                processSpecialTeamStatsCell(header.text, cell)
            }
            .toMap()
    }

    /**
     * Calculates the average of a list of numeric values, rounded to two decimal places.
     * Returns 0.0 if the list is empty.
     *
     * @param values List of numeric values to average
     * @return Average value rounded to two decimal places
     */
    fun calculateAverageWithRounding(values: List<Double>): Double {
        return if (values.isNotEmpty())
            (values.average() * 100).roundToInt() / 100.0
        else 0.0
    }

    /**
     * Assigns weight coefficients to each statistical metric based on its importance.
     *
     * @param stats Team statistics to extract keys from
     * @return Map of statistical keys to their weight values
     */
    private fun getWeights(stats: TeamStats): Map<String, Double> =
        stats.keys.associateWith { key ->
            when (key) {
                "Goles" -> 0.23
                "Tiros pp" -> 0.15
                "Posesion%" -> 0.15
                "AciertoPase%" -> 0.15
                "Aéreos" -> 0.10
                "Rating" -> 0.15
                "Yellow Cards" -> -0.02
                "Red Cards" -> -0.05
                // Advanced statistics added
                "Goals per game" -> 0.25
                "Shot Effectiveness" -> -0.15  // Negative because smaller is better
                "Wins" -> 0.20
                "Draws" -> 0.10
                "Losses" -> -0.20
                else -> 0.0
            }
        }

    /**
     * Calculates a weighted score for a team based on their statistics.
     *
     * @param stats Team statistics to evaluate
     * @param weights Weight coefficients for each statistic
     * @return Composite score representing team strength
     */
    private fun calcScore(stats: TeamStats, weights: Map<String, Double>): Double =
        weights.entries.fold(0.0) { score, (key, weight) ->
            score + stats[key] * weight
        }

    /**
     * Converts a decimal to percentage format rounded to two decimal places.
     */
    private fun Double.toPercentageRounded(): Double =
        (this * 10000).roundToInt() / 100.0

    /**
     * Formats a double value as a string with two decimal places.
     */
    private fun Double.formatTwoDecimals(): String =
        String.format("%.2f", this)

    /**
     * Rounds a double value to two decimal places.
     */
    private fun Double.toRounded(): Double =
        (this * 100).roundToInt() / 100.0

    /**
     * Calculates a score for potential draw based on team scores difference.
     */
    private fun calculateDrawScore(scoreLocal: Double, scoreVisiting: Double): Double {
        val diff = abs(scoreLocal - scoreVisiting)
        val drawFactor = exp(-diff / 3.0)
        val drawMultiplier = 1.2
        return (scoreLocal + scoreVisiting) / 2 * drawFactor * drawMultiplier
    }

    /**
     * Calculates probabilities for each match outcome.
     */
    private fun calculateOutcomeProbabilities(
        scoreLocal: Double,
        scoreDraw: Double,
        scoreVisiting: Double
    ): Map<String, Double> {
        val expScores = listOf(scoreLocal, scoreDraw, scoreVisiting).map { exp(it) }
        val sum = expScores.sum()

        return mapOf(
            "Local Win" to (expScores[0] / sum).toPercentageRounded(),
            "Draw" to (expScores[1] / sum).toPercentageRounded(),
            "Visiting Win" to (expScores[2] / sum).toPercentageRounded()
        )
    }

    /**
     * Groups raw data by key for easier processing.
     */
    private fun groupDataByKey(rawData: List<Pair<String, Double>>): Map<String, List<Double>> {
        return rawData.groupBy({ it.first }, { it.second })
    }

    /**
     * Updates statistics map with new values.
     */
    private fun updateStatsMap(
        rowData: Map<String, List<Double>>,
        currentStats: Map<String, Double>
    ): Map<String, Double> {
        return currentStats + rowData
            .filterKeys { !it.endsWith("_count") }
            .mapValues { (k, values) -> (currentStats[k] ?: 0.0) + values.sum() }
    }

    /**
     * Updates counters map with new values.
     */
    private fun updateCountsMap(
        rowData: Map<String, List<Double>>,
        currentCounts: Map<String, Int>
    ): Map<String, Int> {
        return currentCounts + rowData
            .filterKeys { it.endsWith("_count") }
            .mapValues { (k, values) ->
                (currentCounts[k.removeSuffix("_count")] ?: 0) + values.sum().toInt()
            }
            .mapKeys { it.key.removeSuffix("_count") }
    }
}
