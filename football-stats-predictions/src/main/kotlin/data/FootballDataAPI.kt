package data

import com.fasterxml.jackson.databind.ObjectMapper
import java.net.HttpURLConnection
import java.net.URL

class FootballDataAPI() : StatisticsProvider {

    val apiUrl = "https://api.football-data.org/"

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
        // TODO: Alguna forma de mapear el nombre del equipo al id en football-data con algun mapping
        val requestURL = apiUrl + "v4/teams/$teamName/"
        // TODO: alguna forma copada de manejar las urls con springboot
        val connection = URL(requestURL).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Auth-Token", "APIKEY")
            // TODO: pasar la API KEY de manera segura
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val mapper = ObjectMapper()
        val rootNode = mapper.readTree(response)
        val squadNode = rootNode.get("squad")

        return squadNode.map { playerNode ->
            Player(
                id = playerNode.get("id").asLong(),
                name = playerNode.get("name").asText(),
                position = playerNode.get("position").asText(),
                dateOfBirth = playerNode.get("dateOfBirth").asText(),
                nationality = playerNode.get("nationality").asText()
            )
        }
    }

    // TODO: Diferencias entre esta clase data y la clase de modelo Player? Como convertimos los objetos de una a otra?
    data class Player(
        val id: Long,
        val name: String,
        val position: String,
        val dateOfBirth: String,
        val nationality: String
    )
}