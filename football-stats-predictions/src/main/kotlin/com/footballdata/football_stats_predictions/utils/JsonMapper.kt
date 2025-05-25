package com.footballdata.football_stats_predictions.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.footballdata.football_stats_predictions.model.Match
import com.footballdata.football_stats_predictions.model.Player
import java.text.SimpleDateFormat

//@Component
class JsonMapper {
    // Mapper configuration
    private val mapper: ObjectMapper = ObjectMapper().apply {
        // Adds support for nullable types, data classes, kotlin specific types
        findAndRegisterModules()
        // Disable failure on unknown properties to avoid issues with API changes for resilience
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        // Accepts single values as arrays
        configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        // Avoids failure on null values for primitive types, for numeric types that may be null
        configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
        // Avoids failure on unknown enum values
        configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
        // Consistent date format for serialization
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        // Date format for parsing date strings
        dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    }

    private fun parsePlayer(playerNode: JsonNode) = Player(
        id = playerNode.get("id").asLong(),
        playerName = playerNode.get("name").asText(),
        position = playerNode.get("position").asText(),
        dateOfBirth = playerNode.get("dateOfBirth").asText(),
        nationality = playerNode.get("nationality").asText(),
        shoots = 0,
        interceptions = 0
    )

    private fun parseMatch(matchNode: JsonNode) = Match(
        id = matchNode.get("id").asLong(),
        date = matchNode.get("utcDate").asText(),
        league = matchNode.get("competition").get("name").asText(),
        homeTeamId = matchNode.get("homeTeam").get("id").asLong(),
        homeTeamName = matchNode.get("homeTeam").get("name").asText(),
        awayTeamId = matchNode.get("awayTeam").get("id").asLong(),
        awayTeamName = matchNode.get("awayTeam").get("name").asText()
    )

    fun parseTeamComposition(json: String): List<Player> {
        val rootNode = mapper.readTree(json)
        val squadNode = rootNode.get("squad")
        return squadNode.map { parsePlayer(it) }
    }

    fun parseScheduledMatches(json: String): List<Match> {
        val rootNode = mapper.readTree(json)
        val matchesNode = rootNode.get("matches")
        return matchesNode.map { parseMatch(it) }
    }
}