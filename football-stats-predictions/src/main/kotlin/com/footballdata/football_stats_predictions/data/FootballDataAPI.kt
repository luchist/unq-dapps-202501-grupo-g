package com.footballdata.football_stats_predictions.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.footballdata.football_stats_predictions.model.Match
import com.footballdata.football_stats_predictions.model.Player
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.URL


@Component
class FootballDataAPI(
    @Value("\${integration.football.api.url}") val apiUrl: String,
    @Value("\${integration.football.api.apikey}") val apiKey: String,
    val connectionFactory: (String) -> HttpURLConnection = { urlString: String ->
        URL(urlString).openConnection() as HttpURLConnection
    }
) : StatisticsProvider {

    override fun getTeamStatistics(teamName: String): TeamStatistics {
        TODO("Not yet implemented")
    }

    override fun getPlayerStatistics(playerName: String): PlayerStatistics {
        TODO("Not yet implemented")
    }

    override fun getMatchStatistics(matchId: String): MatchStatistics {
        TODO("Not yet implemented")
    }

    fun getMatchResult(matchDate: String, leagueName: String) {
        TODO("Not yet implemented")
    }

    fun getFixtureLeague(leagueName: String) {
        TODO("Not yet implemented")
    }

    fun getTeamSetup(teamName: String, matchDate: String, leagueName: String) {
        TODO("Not yet implemented")
    }

    fun getTeamComposition(teamName: String): List<Player> {

        val requestURL = apiUrl + "v4/teams/$teamName/"

        val connection = connectionFactory(requestURL)

        connection.apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Auth-Token", apiKey)
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val mapper = ObjectMapper()
        val rootNode = mapper.readTree(response)
        val squadNode = rootNode.get("squad")

        return squadNode.map { playerNode ->
            Player(
                id = playerNode.get("id").asLong(),
                playerName = playerNode.get("name").asText(),
                position = playerNode.get("position").asText(),
                dateOfBirth = playerNode.get("dateOfBirth").asText(),
                nationality = playerNode.get("nationality").asText(),
                shoots = 0,
                interceptions = 0
            )
        }
    }

    fun getScheduledMatches(teamName: String): List<Match> {
        val requestURL = apiUrl + "v4/teams/$teamName/matches?status=SCHEDULED"

        val connection = connectionFactory(requestURL)

        connection.apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Auth-Token", apiKey)
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val mapper = ObjectMapper()
        val rootNode = mapper.readTree(response)
        val matchesNode = rootNode.get("matches")

        return matchesNode.map { matchNode ->
            Match(
                id = matchNode.get("id").asLong(),
                date = matchNode.get("utcDate").asText(),
                league = matchNode.get("competition").get("name").asText(),
                homeTeamId = matchNode.get("homeTeam").get("id").asLong(),
                homeTeamName = matchNode.get("homeTeam").get("name").asText(),
                awayTeamId = matchNode.get("awayTeam").get("id").asLong(),
                awayTeamName = matchNode.get("awayTeam").get("name").asText()
            )
        }
    }
}