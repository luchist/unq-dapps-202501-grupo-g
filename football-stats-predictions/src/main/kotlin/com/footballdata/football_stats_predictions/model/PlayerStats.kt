package com.footballdata.football_stats_predictions.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonValue

data class PlayerStats(
    @JsonValue  // Esta anotación hace que solo este campo se use al serializar
    val data: Map<String, Double>
) : Stats {
    @JsonIgnore
    override operator fun get(key: String): Double = data[key] ?: 0.0

    @get:JsonIgnore
    override val keys: Set<String>
        get() = data.keys

    @JsonIgnore
    operator fun plus(other: PlayerStats): PlayerStats {
        return PlayerStats(this.data + other.data)
    }

    @JsonIgnore
    fun isEmpty(): Boolean {
        return data.isEmpty()
    }
}