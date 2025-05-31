package com.footballdata.football_stats_predictions.unittest.service

import com.footballdata.football_stats_predictions.model.Role
import com.footballdata.football_stats_predictions.model.User
import com.footballdata.football_stats_predictions.repositories.UserRepository
import com.footballdata.football_stats_predictions.service.JwtUserDetailsService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.util.*

class JwtUserDetailsServiceTest {
    private lateinit var userDetailsService: JwtUserDetailsService
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        userDetailsService = JwtUserDetailsService(userRepository)
    }

    @Test
    fun `loadUserByUsername should return UserDetails when user exists`() {
        // Arrange
        val username = "testUser"
        val password = "testPassword"
        val mockUser = User(name = username, password = password, role = Role.USER, id = UUID.randomUUID())
        `when`(userRepository.findByUsername(username)).thenReturn(mockUser)

        // Act
        val userDetails = userDetailsService.loadUserByUsername(username)

        // Assert
        assertEquals(username, userDetails.username)
        assertEquals(password, userDetails.password)
        assertEquals(1, userDetails.authorities.size)
        assertEquals("ROLE_USER", userDetails.authorities.first().authority)
    }

    @Test
    fun `loadUserByUsername should throw UsernameNotFoundException when user does not exist`() {
        // Arrange
        val username = "nonExistentUser"
        `when`(userRepository.findByUsername(username)).thenReturn(null)

        // Act & Assert
        val exception = assertThrows<UsernameNotFoundException> {
            userDetailsService.loadUserByUsername(username)
        }
        assertEquals("User $username not found!", exception.message)
    }
}