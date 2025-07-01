package com.footballdata.football_stats_predictions.model

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*

@Entity
data class MatchPrediction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val localTeam: String = "",
    val awayTeam: String = "",

    @Column(columnDefinition = "TEXT")
    @Convert(converter = PredictionDataConverter::class)
    val predictions: Map<String, Double> = mapOf()
)

@Converter
class PredictionDataConverter : AttributeConverter<Map<String, Double>, String> {
    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: Map<String, Double>): String {
        return try {
            objectMapper.writeValueAsString(attribute)
        } catch (_: Exception) {
            "{}"
        }
    }

    override fun convertToEntityAttribute(dbData: String): Map<String, Double> {
        return try {
            objectMapper.readValue(dbData, object : TypeReference<Map<String, Double>>() {})
        } catch (_: Exception) {
            mapOf()
        }
    }
}
