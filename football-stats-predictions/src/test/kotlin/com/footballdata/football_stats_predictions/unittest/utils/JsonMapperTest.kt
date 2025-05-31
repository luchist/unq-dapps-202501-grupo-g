package com.footballdata.football_stats_predictions.unittest.utils

import com.footballdata.football_stats_predictions.utils.JsonMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JsonMapperTest {
    private val jsonMapper = JsonMapper()

    @Test
    fun `should parse player correctly with all fields`() {
        val json = """
        {
            "squad": [{
                "id": 1,
                "name": "Lionel Messi",
                "position": "Forward",
                "dateOfBirth": "1987-06-24",
                "nationality": "Argentina"
            }]
        }
        """.trimIndent()

        val result = jsonMapper.parseTeamComposition(json)

        assertEquals(1, result.size)
        with(result[0]) {
            assertEquals(1L, id)
            assertEquals("Lionel Messi", playerName)
            assertEquals("Forward", position)
            assertEquals("1987-06-24", dateOfBirth)
            assertEquals("Argentina", nationality)
        }
    }

    @Test
    fun `should parse match with minimum required fields`() {
        val json = """
        {
            "matches": [{
                "id": 1,
                "utcDate": "2024-03-20T20:00:00Z",
                "competition": { "name": "La Liga" },
                "homeTeam": { "id": 81, "name": "Barcelona" },
                "awayTeam": { "id": 86, "name": "Real Madrid" }
            }]
        }
        """.trimIndent()

        val result = jsonMapper.parseScheduledMatches(json)

        assertEquals(1, result.size)
        with(result[0]) {
            assertEquals(1L, id)
            assertEquals("2024-03-20T20:00:00Z", date)
            assertEquals("La Liga", league)
            assertEquals(81L, homeTeamId)
            assertEquals("Barcelona", homeTeamName)
            assertEquals(86L, awayTeamId)
            assertEquals("Real Madrid", awayTeamName)
        }
    }

    @Test
    fun `should handle empty team composition`() {
        val json = """{ "squad": [] }"""
        val result = jsonMapper.parseTeamComposition(json)
        assertEquals(0, result.size)
    }

    @Test
    fun `should handle malformed JSON for team composition`() {
        val invalidJson = "{ invalid json }"
        assertThrows<Exception> {
            jsonMapper.parseTeamComposition(invalidJson)
        }
    }

    @Test
    fun `should handle empty scheduled matches`() {
        val json = """{ "matches": [] }"""
        val result = jsonMapper.parseScheduledMatches(json)
        assertEquals(0, result.size)
    }

    @Test
    fun `should handle malformed JSON for scheduled matches`() {
        val invalidJson = "{ invalid json }"
        assertThrows<Exception> {
            jsonMapper.parseScheduledMatches(invalidJson)
        }
    }
}