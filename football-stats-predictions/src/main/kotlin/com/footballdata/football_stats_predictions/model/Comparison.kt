package com.footballdata.football_stats_predictions.model

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
data class Comparison(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // Tipo de comparación (TEAM o PLAYER)
    @Enumerated(EnumType.STRING)
    val comparisonType: ComparisonType,

    // Identificadores de las entidades comparadas
    val entity1Name: String = "",
    val entity2Name: String = "",

    // Fecha de creación para control de caché
    val createdAt: LocalDateTime = LocalDateTime.now(),

    // Datos de la comparación
    @Column(columnDefinition = "TEXT")
    @Convert(converter = ComparisonDataConverter::class)
    val comparisonData: Map<String, Map<String, String>> = mapOf()
)

enum class ComparisonType {
    TEAM,
    PLAYER
}

@Converter
class ComparisonDataConverter : AttributeConverter<Map<String, Map<String, String>>, String> {
    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: Map<String, Map<String, String>>): String {
        return try {
            objectMapper.writeValueAsString(attribute)
        } catch (_: Exception) {
            "{}"
        }
    }

    override fun convertToEntityAttribute(dbData: String): Map<String, Map<String, String>> {
        return try {
            objectMapper.readValue(dbData, object : TypeReference<Map<String, Map<String, String>>>() {})
        } catch (_: Exception) {
            mapOf()
        }
    }
}