package com.footballdata.football_stats_predictions.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*

@Entity
data class PlayerStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val playerName: String = "",

    @Column(columnDefinition = "TEXT")
    @Convert(converter = PlayerStatsDataConverter::class)
    @JsonValue
    val data: Map<String, Double> = mapOf()
) : Stats {
    @JsonIgnore
    override operator fun get(key: String): Double = data[key] ?: 0.0

    @get:JsonIgnore
    override val keys: Set<String>
        get() = data.keys

    @JsonIgnore
    operator fun plus(other: PlayerStats): PlayerStats {
        return PlayerStats(id, playerName, this.data + other.data)
    }

    @JsonIgnore
    fun isEmpty(): Boolean {
        return data.isEmpty()
    }
}

@Converter
class PlayerStatsDataConverter : AttributeConverter<Map<String, Double>, String> {
    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: Map<String, Double>): String {
        return try {
            objectMapper.writeValueAsString(attribute)
        } catch (e: Exception) {
            "{}"
        }
    }

    override fun convertToEntityAttribute(dbData: String): Map<String, Double> {
        return try {
            objectMapper.readValue(dbData, object : TypeReference<Map<String, Double>>() {})
        } catch (e: Exception) {
            mapOf()
        }
    }
}

class PlayerStatsBuilder(
    private var id: Long = 0,
    private var playerName: String = "",
    private var data: Map<String, Double> = mapOf()
) {
    fun withId(id: Long) = apply { this.id = id }
    fun withPlayerName(playerName: String) = apply { this.playerName = playerName }
    fun withData(data: Map<String, Double>) = apply { this.data = data }

    fun build() = PlayerStats(
        id = id,
        playerName = playerName,
        data = data
    )
}