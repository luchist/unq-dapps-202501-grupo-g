package com.footballdata.football_stats_predictions.unit.repositories

import com.footballdata.football_stats_predictions.repositories.RefreshTokenRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.core.userdetails.UserDetails

class RefreshTokenRepositoryTest {

    private lateinit var repository: RefreshTokenRepository

    @BeforeEach
    fun setUp() {
        repository = RefreshTokenRepository()
    }

    @Test
    fun `save should store token with user details`() {
        // Arrange
        val token = "test.token"
        val userDetails = mock(UserDetails::class.java)

        // Act
        repository.save(token, userDetails)
        val result = repository.findUserDetailsByToken(token)

        // Assert
        assertNotNull(result)
        assertEquals(userDetails, result)
    }

    @Test
    fun `findUserDetailsByToken should return null for non-existent token`() {
        // Act
        val result = repository.findUserDetailsByToken("non.existent.token")

        // Assert
        assertNull(result)
    }

    @Test
    fun `save should override existing token`() {
        // Arrange
        val token = "test.token"
        val userDetails1 = mock(UserDetails::class.java).apply {
            `when`(username).thenReturn("user1")
        }
        val userDetails2 = mock(UserDetails::class.java).apply {
            `when`(username).thenReturn("user2")
        }

        // Act
        repository.save(token, userDetails1)
        repository.save(token, userDetails2)
        val result = repository.findUserDetailsByToken(token)

        // Assert
        assertEquals(userDetails2, result)
    }
}