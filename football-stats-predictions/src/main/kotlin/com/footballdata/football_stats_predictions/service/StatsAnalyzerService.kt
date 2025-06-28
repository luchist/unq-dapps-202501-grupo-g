package com.footballdata.football_stats_predictions.service

import com.footballdata.football_stats_predictions.model.Stats
import com.footballdata.football_stats_predictions.model.TeamStats
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
     * Calculates advanced goal and shot effectiveness metrics for a team.
     *
     * @param stats The base team statistics to analyze
     * @return TeamStats object containing derived metrics: Goals per game and Shot Effectiveness
     */
    fun getTeamGoalsAndShotEffectiveness(stats: TeamStats): TeamStats =
        stats.let { teamStats ->
            val apps = teamStats["Apps"].takeIf { it != 0.0 } ?: 1.0
            TeamStats(
                mapOf(
                    "Goals per game" to (teamStats["Goles"] / apps),
                    "Shot Effectiveness" to (if (teamStats["Goles"] > 0.0) teamStats["Tiros pp"] / (teamStats["Goles"] / apps) else 0.0)
                )
            )
        }

    /**
     * Predicts the probabilities of match outcomes between two teams.
     * Uses weighted statistical models to calculate win/draw probabilities.
     *
     * @param localStats Statistics of the home team
     * @param visitingStats Statistics of the away team
     * @return Map containing the probability percentages for each possible outcome (Local Win, Draw, Visiting Win)
     */
    fun predictMatch(localStats: TeamStats, visitingStats: TeamStats): Map<String, Double> =
        getWeights(localStats).let { weights ->
            val scoreLocal = calcScore(localStats, weights)
            val scoreVisiting = calcScore(visitingStats, weights)

            // Calculate draw factor and score
            abs(scoreLocal - scoreVisiting).let { diff ->
                exp(-diff / 5.0).let { drawFactor ->
                    (scoreLocal + scoreVisiting) / 2 * drawFactor
                }
            }.let { scoreDraw ->
                // Calculate probabilities using exponential model
                listOf(scoreLocal, scoreDraw, scoreVisiting)
                    .map { exp(it) }
                    .let { (expLocal, expDraw, expVisiting) ->
                        val sum = expLocal + expDraw + expVisiting
                        mapOf(
                            "Local Win" to (expLocal / sum).toPercentageRounded(),
                            "Draw" to (expDraw / sum).toPercentageRounded(),
                            "Visiting Win" to (expVisiting / sum).toPercentageRounded()
                        )
                    }
            }
        }


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
                else -> 0.0
            }
        }

    private fun calcScore(stats: TeamStats, weights: Map<String, Double>): Double =
        weights.entries.fold(0.0) { score, (key, weight) ->
            score + stats[key] * weight
        }

    private fun Double.toPercentageRounded(): Double =
        (this * 10000).roundToInt() / 100.0

    private fun Double.formatTwoDecimals(): String =
        String.format("%.2f", this)

}
