package com.footballdata.football_stats_predictions.unit.data

import com.footballdata.football_stats_predictions.data.FootballDataAPI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection

class FootballDataAPITest {

    private lateinit var connection: HttpURLConnection
    private lateinit var footballDataAPI: FootballDataAPI
    private val apiUrl = "http://test-api.com/"
    private val apiKey = "test-api-key"

    @BeforeEach
    fun setup() {
        connection = mock(HttpURLConnection::class.java)
        footballDataAPI = FootballDataAPI(apiUrl, apiKey) { connection }
    }

    @Test
    fun `should parse team composition correctly when API returns valid data`() {
        // Arrange
        val mockJson = """
        {
            "squad": [
                {
                    "id": 1,
                    "name": "Lionel Messi",
                    "position": "Forward",
                    "dateOfBirth": "1987-06-24",
                    "nationality": "Argentina"
                },
                {
                    "id": 2,
                    "name": "Cristiano Ronaldo",
                    "position": "Forward",
                    "dateOfBirth": "1985-02-05",
                    "nationality": "Portugal"
                }
            ]
        }
        """.trimIndent()

        `when`(connection.inputStream).thenReturn(ByteArrayInputStream(mockJson.toByteArray()))
        `when`(connection.responseCode).thenReturn(200)

        // Act
        val result = footballDataAPI.getTeamComposition("90")

        // Assert
        assertEquals(2, result.size)
        with(result[0]) {
            assertEquals(1L, id)
            assertEquals("Lionel Messi", playerName)
            assertEquals("Forward", position)
            assertEquals("1987-06-24", dateOfBirth)
            assertEquals("Argentina", nationality)
        }
    }

    @Test
    fun `should throw exception when API returns error response`() {
        // Arrange
        val connection = mock(HttpURLConnection::class.java)
        `when`(connection.responseCode).thenReturn(404)

        // Act & Assert
        assertThrows<Exception> {
            footballDataAPI.getTeamComposition("invalid-team")
        }
    }

    @Test
    fun `should throw exception when API returns invalid JSON`() {
        // Arrange
        val invalidJson = "{ invalid json }"
        val connection = mock(HttpURLConnection::class.java)
        `when`(connection.inputStream).thenReturn(ByteArrayInputStream(invalidJson.toByteArray()))

        // Act & Assert
        assertThrows<Exception> {
            footballDataAPI.getTeamComposition("90")
        }
    }

    @Test
    fun `should parse scheduled matches correctly when API returns valid data`() {
        // Arrange
        val mockJson = """
    {
        "matches": [
            {
                "area": {
                    "id": 2072,
                    "name": "England",
                    "code": "ENG",
                    "flag": "https://crests.football-data.org/770.svg"
                },
                "competition": {
                    "id": 2021,
                    "name": "Premier League",
                    "code": "PL",
                    "type": "LEAGUE",
                    "emblem": "https://crests.football-data.org/PL.png"
                },
                "season": {
                    "id": 2287,
                    "startDate": "2024-08-16",
                    "endDate": "2025-05-25",
                    "currentMatchday": 38,
                    "winner": null
                },
                "id": 497781,
                "utcDate": "2025-07-25T15:00:00Z",
                "status": "TIMED",
                "matchday": 38,
                "stage": "REGULAR_SEASON",
                "group": null,
                "lastUpdated": "2025-05-25T00:20:40Z",
                "homeTeam": {
                    "id": 63,
                    "name": "Fulham FC",
                    "shortName": "Fulham",
                    "tla": "FUL",
                    "crest": "https://crests.football-data.org/63.png"
                },
                "awayTeam": {
                    "id": 65,
                    "name": "Manchester City FC",
                    "shortName": "Man City",
                    "tla": "MCI",
                    "crest": "https://crests.football-data.org/65.png"
                },
                "score": {
                    "winner": null,
                    "duration": "REGULAR",
                    "fullTime": {
                        "home": null,
                        "away": null
                    },
                    "halfTime": {
                        "home": null,
                        "away": null
                    }
                }
            },
            {
                "id": 497789,
                "utcDate": "2026-09-25T15:00:00Z",
                "status": "SCHEDULED",
                "competition": {
                    "name": "Champions League"
                },
                "homeTeam": {
                    "id": 86,
                    "name": "Real Madrid"
                },
                "awayTeam": {
                    "id": 65,
                    "name": "Manchester City"
                }
            }
        ]
    }
    """.trimIndent()

        `when`(connection.inputStream).thenReturn(ByteArrayInputStream(mockJson.toByteArray()))
        `when`(connection.responseCode).thenReturn(200)

        // Act
        val result = footballDataAPI.getScheduledMatches("65")

        // Assert
        assertEquals(2, result.size)
        with(result[0]) {
            assertEquals(497781L, id)
            assertEquals("2025-07-25T15:00:00Z", date)
            assertEquals("Premier League", league)
            assertEquals("Fulham FC", homeTeamName)
            assertEquals("Manchester City FC", awayTeamName)
        }
        with(result[1]) {
            assertEquals(497789L, id)
            assertEquals("2026-09-25T15:00:00Z", date)
            assertEquals("Champions League", league)
            assertEquals("Real Madrid", homeTeamName)
            assertEquals("Manchester City", awayTeamName)
        }
    }

    @Test
    fun `should throw exception when API returns error response for scheduled matches`() {
        // Arrange
        `when`(connection.responseCode).thenReturn(404)

        // Act & Assert
        assertThrows<Exception> {
            footballDataAPI.getScheduledMatches("invalid-team")
        }
    }

    @Test
    fun `should throw exception when API returns invalid JSON for scheduled matches`() {
        // Arrange
        val invalidJson = "{ invalid json }"
        `when`(connection.inputStream).thenReturn(ByteArrayInputStream(invalidJson.toByteArray()))
        `when`(connection.responseCode).thenReturn(200)

        // Act & Assert
        assertThrows<Exception> {
            footballDataAPI.getScheduledMatches("86")
        }
    }
}