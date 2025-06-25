package com.footballdata.football_stats_predictions.unit.service

import com.footballdata.football_stats_predictions.service.TokenService
import io.jsonwebtoken.ExpiredJwtException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class TokenServiceTest {
    private lateinit var tokenService: TokenService

    // random secret key for testing purposes
    private val secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
    private val testUsername = "testUser"

    @BeforeEach
    fun setUp() {
        tokenService = TokenService(secret)
    }

    @Test
    fun `generateToken should create valid token with correct claims`() {
        // Arrange
        val expiration = Date(System.currentTimeMillis() + 1000 * 60) // 1 minute
        val additionalClaims = mapOf("role" to "USER")

        // Act
        val token = tokenService.generateToken(testUsername, expiration, additionalClaims)

        // Assert
        assertNotNull(token)
        assertEquals(testUsername, tokenService.extractUsername(token))
    }

    @Test
    fun `generateToken should create valid token without additional claims`() {
        // Arrange
        val expiration = Date(System.currentTimeMillis() + 1000 * 60)

        // Act
        val token = tokenService.generateToken(testUsername, expiration)

        // Assert
        assertNotNull(token)
        assertEquals(testUsername, tokenService.extractUsername(token))
    }

    @Test
    fun `extractUsername should throw exception when token is expired`() {
        // Arrange
        val expiration = Date(System.currentTimeMillis() - 1000) // Expired token
        val token = tokenService.generateToken(testUsername, expiration)

        // Act & Assert
        assertThrows<ExpiredJwtException> {
            tokenService.extractUsername(token)
        }
    }

    @Test
    fun `extractUsername should throw exception when token is invalid`() {
        // Arrange
        val invalidToken = "invalid.token.string"

        // Act & Assert
        assertThrows<Exception> {
            tokenService.extractUsername(invalidToken)
        }
    }
}