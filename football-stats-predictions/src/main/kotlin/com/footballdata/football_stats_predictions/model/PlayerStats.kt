package com.footballdata.football_stats_predictions.model

data class PlayerStats(val data: Map<String, Double>) : Stats {
    override operator fun get(key: String): Double = data[key] ?: 0.0

    override val keys: Set<String>
        get() = data.keys

    operator fun plus(other: PlayerStats): PlayerStats {
        return PlayerStats(this.data + other.data)
    }

    fun isEmpty(): Boolean {
        return data.isEmpty()
    }
}