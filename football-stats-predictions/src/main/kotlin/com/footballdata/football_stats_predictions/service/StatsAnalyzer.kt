package com.footballdata.football_stats_predictions.service

import com.footballdata.football_stats_predictions.data.FootballDataScraping
import com.footballdata.football_stats_predictions.model.Stats
import com.footballdata.football_stats_predictions.model.TeamStats
import org.springframework.stereotype.Service
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

@Service
class StatsAnalyzer(
    private val footballDataScraping: FootballDataScraping
) {

    fun compareStatsWithDiff(
        stats1: Stats,
        stats2: Stats,
        key1: String,
        key2: String
    ): Map<String, Map<String, String>> {
        val allKeys = stats1.keys + stats2.keys

        val map1 = mutableMapOf<String, String>()
        val map2 = mutableMapOf<String, String>()

        for (key in allKeys) {
            val v1 = stats1[key]
            val v2 = stats2[key]
            val diff = v1 - v2
            map1[key] = "$v1 (${diff.formatTwoDecimals()})"
            map2[key] = "$v2 (${(-diff).formatTwoDecimals()})"
        }

        return mapOf(
            key1 to map1,
            key2 to map2
        )
    }

    fun getTeamGoalsAndShotEffectiveness(stats: TeamStats): TeamStats {
        val goals = stats["Goles"]
        val apps = stats["Apps"].takeIf{ it != 0.0 } ?: 1.0 // Evita división por cero
        val tirosPP = stats["Tiros pp"]

        val goalsAGame = goals / apps
        val efectividadDeTiros = if (goalsAGame != 0.0) tirosPP / goalsAGame else 0.0

        return TeamStats(mapOf(
            "Goles por Partido" to goalsAGame,
            "Efectividad de Tiros" to efectividadDeTiros
        ))
    }

    fun predictMatchProbabilities(localTeam: String, visitingTeam: String): Map<String, Double> {
        val localStats = footballDataScraping.getTeamData(localTeam)
        val visitingStats = footballDataScraping.getTeamData(visitingTeam)

        val weights = getWeights(localStats)

        val scoreLocal = calcScore(localStats, weights)
        val scoreVisiting = calcScore(visitingStats, weights)

        val diff = abs(scoreLocal - scoreVisiting)
        val drawFactor = exp(-diff / 5.0)
        val scoreDraw = (scoreLocal + scoreVisiting) / 2 * drawFactor

        val expLocal = exp(scoreLocal)
        val expDraw = exp(scoreDraw)
        val expVisiting = exp(scoreVisiting)
        val sum = expLocal + expDraw + expVisiting

        // Calculate probabilities
        val probLocal = (expLocal / sum).toPercentageRounded()
        val probDraw = (expDraw / sum).toPercentageRounded()
        val probVisiting = (expVisiting / sum).toPercentageRounded()

        return mapOf(
            "Local Win" to probLocal,
            "Draw" to probDraw,
            "Visiting Win" to probVisiting
        )
    }

    private fun getWeights(stats: TeamStats): Map<String, Double> {
        val weights = mutableMapOf<String, Double>()
        for (key in stats.keys) {
            val value = when (key) {
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
            weights[key] = value
        }
        return weights
    }

    private fun calcScore(stats: TeamStats, weights: Map<String, Double>): Double {
        var score = 0.0
        for ((key, weight) in weights) {
            val value = stats[key]
            score += value * weight
        }
        return score
    }

    private fun Double.toPercentageRounded(): Double =
        (this * 10000).roundToInt() / 100.0

    private fun Double.formatTwoDecimals(): String =
        String.format("%.2f", this)

}
