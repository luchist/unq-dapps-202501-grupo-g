package com.footballdata.football_stats_predictions.unittest.webservice

import com.footballdata.football_stats_predictions.dto.*
import com.footballdata.football_stats_predictions.model.Role
import com.footballdata.football_stats_predictions.repositories.UserRepository
import com.footballdata.football_stats_predictions.service.AuthenticationService
import com.footballdata.football_stats_predictions.webservice.AuthController
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder

@ExtendWith(MockitoExtension::class)
class AuthControllerTest {
    @Mock
    private lateinit var authenticationService: AuthenticationService

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var authController: AuthController

    @Test
    fun `authenticate should return authentication response`() {
        // Arrange
        val request = AuthenticationRequest("testUser", "password123")
        val expectedResponse = AuthenticationResponse("token", "refreshToken")
        `when`(authenticationService.authentication(request))
            .thenReturn(expectedResponse)

        // Act
        val response = authController.authenticate(request)

        // Assert
        assertEquals(expectedResponse, response)
        verify(authenticationService).authentication(request)
    }

    @Test
    fun `authenticate should throw exception when service fails`() {
        // Arrange
        val request = AuthenticationRequest("testUser", "password123")
        `when`(authenticationService.authentication(request))
            .thenThrow(RuntimeException("Authentication failed"))

        // Act & Assert
        assertThrows<RuntimeException> {
            authController.authenticate(request)
        }
    }

    @Test
    fun `refreshAccessToken should return token response`() {
        // Arrange
        val refreshToken = "refreshToken"
        val request = RefreshTokenRequest(refreshToken)
        val newAccessToken = "newAccessToken"
        `when`(authenticationService.refreshAccessToken(refreshToken)).thenReturn(newAccessToken)

        // Act
        val response = authController.refreshAccessToken(request)

        // Assert
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(TokenResponse(token = newAccessToken), response.body)
    }

    @Test
    fun `refreshAccessToken should throw exception when token is invalid`() {
        // Arrange
        val request = RefreshTokenRequest("invalidToken")
        `when`(authenticationService.refreshAccessToken(request.token))
            .thenThrow(RuntimeException("Invalid token"))

        // Act & Assert
        assertThrows<RuntimeException> {
            authController.refreshAccessToken(request)
        }
    }

    @Test
    fun `register should create user and return auth response`() {
        // Arrange
        val request = RegisterRequest("testUser", "password123")
        val expectedResponse = AuthenticationResponse("token", "refreshToken")
        val hashedPassword = "hashedPassword"

        `when`(passwordEncoder.encode(request.password))
            .thenReturn(hashedPassword)
        `when`(authenticationService.authentication(any()))
            .thenReturn(expectedResponse)

        // Act
        val response = authController.register(request)

        // Assert
        verify(userRepository).save(any())
        assertEquals(expectedResponse, response)
    }

    @Test
    fun `register should create user with correct data`() {
        // Arrange
        val request = RegisterRequest("testUser", "password123")
        val hashedPassword = "hashedPassword"
        val expectedResponse = AuthenticationResponse("token", "refreshToken")

        `when`(passwordEncoder.encode(request.password))
            .thenReturn(hashedPassword)
        `when`(authenticationService.authentication(any()))
            .thenReturn(expectedResponse)

        // Act
        authController.register(request)

        // Assert
        verify(userRepository).save(argThat { user ->
            user.name == request.username &&
                    user.password == hashedPassword &&
                    user.role == Role.USER
        })
    }

    @Test
    fun `register should throw exception when username already exists`() {
        // Arrange
        val request = RegisterRequest("existingUser", "password123")
        val hashedPassword = "hashedPassword"

        `when`(passwordEncoder.encode(request.password)).thenReturn(hashedPassword)
        `when`(userRepository.save(any())).thenThrow(RuntimeException("User already exists"))

        // Act & Assert
        assertThrows<RuntimeException> {
            authController.register(request)
        }

        // Verify
        verify(passwordEncoder).encode(request.password)
    }
}