package com.footballdata.football_stats_predictions.model

interface Stats {
    operator fun get(key: String): Double
    val keys: Set<String>
}