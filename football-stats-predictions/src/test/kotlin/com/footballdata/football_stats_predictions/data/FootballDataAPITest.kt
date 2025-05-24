package com.footballdata.football_stats_predictions.data

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
}