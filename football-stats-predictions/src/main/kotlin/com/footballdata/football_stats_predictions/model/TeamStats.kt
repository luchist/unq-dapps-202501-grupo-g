package com.footballdata.football_stats_predictions.model

data class TeamStats(val data: Map<String, Double>) : Stats {
    override operator fun get(key: String): Double = data[key] ?: 0.0

    override val keys: Set<String>
        get() = data.keys

    operator fun plus(other: TeamStats): TeamStats {
        return TeamStats(this.data + other.data)
    }

    fun isEmpty(): Boolean {
        return data.isEmpty()
    }
}