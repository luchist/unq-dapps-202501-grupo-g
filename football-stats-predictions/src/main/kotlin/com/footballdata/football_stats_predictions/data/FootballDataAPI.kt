package com.footballdata.football_stats_predictions.data

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.footballdata.football_stats_predictions.model.Match
import com.footballdata.football_stats_predictions.model.Player
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat


@Component
class FootballDataAPI(
    @Value("\${integration.football.api.url}") val apiUrl: String,
    @Value("\${integration.football.api.apikey}") val apiKey: String,
    val connectionFactory: (String) -> HttpURLConnection = { urlString: String ->
        URL(urlString).openConnection() as HttpURLConnection
    }
) {

    private val mapper: ObjectMapper = ObjectMapper()

    init {
        // Mapper configuration
        // Adds support for nullable types, data classes, kotlin specific types
        mapper.findAndRegisterModules()
        // Disable failure on unknown properties to avoid issues with API changes for resilience
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        // Accepts single values as arrays
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        // Avoids failure on null values for primitive types, for numeric types that may be null
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
        // Avoids failure on unknown enum values
        mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
        // Consistent date format for serialization
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        // Date format for parsing date strings
        mapper.dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    }

    private fun buildUrl(endpoint: String): String {
        val baseUrl = apiUrl.removeSuffix("/")
        val cleanEndpoint = endpoint.removePrefix("/")
        return "$baseUrl/$cleanEndpoint"
    }

    private fun makeApiRequest(endpoint: String): String {
        val requestURL = buildUrl(endpoint)
        val connection = connectionFactory(requestURL)

        return try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-Auth-Token", apiKey)
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw RuntimeException("API request failed: ${connection.responseCode}")
            }

            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
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

    fun getTeamComposition(teamName: String): List<Player> {

        val requestURL = "v4/teams/$teamName/"

        val response = makeApiRequest(requestURL)
        val rootNode = mapper.readTree(response)
        val squadNode = rootNode.get("squad")

        return squadNode.map { playerNode ->
            parsePlayer(playerNode)
        }
    }

    fun getScheduledMatches(teamName: String): List<Match> {
        val requestURL = apiUrl + "v4/teams/$teamName/matches?status=SCHEDULED"

        val response = makeApiRequest(requestURL)
        val rootNode = mapper.readTree(response)
        val matchesNode = rootNode.get("matches")

        return matchesNode.map { matchNode ->
            parseMatch(matchNode)
        }
    }
}