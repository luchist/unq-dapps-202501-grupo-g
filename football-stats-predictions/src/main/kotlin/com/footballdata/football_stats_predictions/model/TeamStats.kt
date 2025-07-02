package com.footballdata.football_stats_predictions.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*

@Entity
data class TeamStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val teamName: String = "",

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StatsDataConverter::class)
    @JsonValue
    val data: Map<String, Double> = mapOf()
) : Stats {
    @JsonIgnore
    override operator fun get(key: String): Double = data[key] ?: 0.0

    @get:JsonIgnore
    override val keys: Set<String>
        get() = data.keys

    @JsonIgnore
    operator fun plus(other: TeamStats): TeamStats {
        return TeamStats(id, teamName, this.data + other.data)
    }

    @JsonIgnore
    fun isEmpty(): Boolean {
        return data.isEmpty()
    }
}

@Converter
class StatsDataConverter : AttributeConverter<Map<String, Double>, String> {
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

class TeamStatsBuilder(
    private var id: Long = 0,
    private var teamName: String = "",
    private var data: Map<String, Double> = mapOf()
) {
    fun withId(id: Long) = apply { this.id = id }
    fun withTeamName(teamName: String) = apply { this.teamName = teamName }
    fun withData(data: Map<String, Double>) = apply { this.data = data }

    fun build() = TeamStats(
        id = id,
        teamName = teamName,
        data = data
    )
}