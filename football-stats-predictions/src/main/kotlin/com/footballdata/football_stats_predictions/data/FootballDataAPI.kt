package com.footballdata.football_stats_predictions.data

import com.footballdata.football_stats_predictions.model.Match
import com.footballdata.football_stats_predictions.model.Player
import com.footballdata.football_stats_predictions.utils.JsonMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.URL

@Component
class FootballDataAPI(
    @Value("\${integration.football.api.url}") val apiUrl: String,
    @Value("\${integration.football.api.apikey}") val apiKey: String,
    private val mapper: JsonMapper = JsonMapper(),
    val connectionFactory: (String) -> HttpURLConnection = { urlString: String ->
        URL(urlString).openConnection() as HttpURLConnection
    }
) {

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

    fun getTeamComposition(teamName: String): List<Player> {
        val requestURL = "v4/teams/$teamName/"
        val response = makeApiRequest(requestURL)

        return mapper.parseTeamComposition(response)
    }

    fun getScheduledMatches(teamName: String): List<Match> {
        val requestURL = apiUrl + "v4/teams/$teamName/matches?status=SCHEDULED"
        val response = makeApiRequest(requestURL)

        return mapper.parseScheduledMatches(response)
    }
}