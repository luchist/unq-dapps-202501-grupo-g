package com.footballdata.football_stats_predictions.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlayerTest {

    @Test
    fun `should create player with correct attributes when using builder`() {
        // Arrange
        val expectedName = "Cristiano Ronaldo"
        val expectedPosition = "Forward"
        val expectedShoots = 10
        val expectedInterceptions = 2
        val expectedDateOfBirth = "05/02/1985"
        val expectedNationality = "Portugal"

        // Act
        val player = PlayerBuilder()
            .withPlayerName(expectedName)
            .withPosition(expectedPosition)
            .withShoots(expectedShoots)
            .withInterceptions(expectedInterceptions)
            .withDateOfBirth(expectedDateOfBirth)
            .withNationality(expectedNationality)
            .build()

        // Assert
        assertEquals(expectedName, player.playerName)
        assertEquals(expectedPosition, player.position)
        assertEquals(expectedShoots, player.shoots)
        assertEquals(expectedInterceptions, player.interceptions)
        assertEquals(expectedDateOfBirth, player.dateOfBirth)
        assertEquals(expectedNationality, player.nationality)
    }

    @Test
    fun `should create player with default values when using empty builder`() {
        // Act
        val player = PlayerBuilder().build()

        // Assert
        assertEquals(0L, player.id)
        assertEquals("", player.playerName)
        assertEquals("", player.position)
        assertEquals("", player.dateOfBirth)
        assertEquals("", player.nationality)
        assertEquals(0, player.shoots)
        assertEquals(0, player.interceptions)
    }
}