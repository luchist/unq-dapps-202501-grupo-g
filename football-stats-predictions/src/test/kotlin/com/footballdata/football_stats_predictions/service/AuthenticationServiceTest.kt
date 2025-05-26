package com.footballdata.football_stats_predictions.service

import com.footballdata.football_stats_predictions.dto.AuthenticationRequest
import com.footballdata.football_stats_predictions.repositories.RefreshTokenRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService

class AuthenticationServiceTest {
    private lateinit var authenticationService: AuthenticationService
    private lateinit var authManager: AuthenticationManager
    private lateinit var userDetailsService: UserDetailsService
    private lateinit var tokenService: TokenService
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    private val accessTokenExpiration = 300000L // 5 minutos
    private val refreshTokenExpiration = 3600000L // 1 hora
    private val testUsername = "testUser"
    private val testPassword = "testPassword"

    @BeforeEach
    fun setUp() {
        authManager = mock(AuthenticationManager::class.java)
        userDetailsService = mock(UserDetailsService::class.java)
        tokenService = mock(TokenService::class.java)
        refreshTokenRepository = mock(RefreshTokenRepository::class.java)

        authenticationService = AuthenticationService(
            authManager,
            userDetailsService,
            tokenService,
            refreshTokenRepository,
            accessTokenExpiration,
            refreshTokenExpiration
        )
    }

    @Test
    fun `authentication should throw exception when credentials are invalid`() {
        // Arrange
        val authRequest = AuthenticationRequest(testUsername, testPassword)

        `when`(authManager.authenticate(any<UsernamePasswordAuthenticationToken>()))
            .thenThrow(AuthenticationServiceException("Invalid credentials"))

        // Act & Assert
        assertThrows<AuthenticationServiceException> {
            authenticationService.authentication(authRequest)
        }
    }

    @Test
    fun `refreshAccessToken should throw exception when refresh token is invalid`() {
        // Arrange
        val invalidRefreshToken = "invalid.refresh.token"
        val mockUserDetails = mock(UserDetails::class.java).apply {
            `when`(username).thenReturn(testUsername)
        }
        val differentUserDetails = mock(UserDetails::class.java).apply {
            `when`(username).thenReturn("different")
        }

        `when`(tokenService.extractUsername(invalidRefreshToken))
            .thenReturn(testUsername)
        `when`(userDetailsService.loadUserByUsername(testUsername))
            .thenReturn(mockUserDetails)
        `when`(refreshTokenRepository.findUserDetailsByToken(invalidRefreshToken))
            .thenReturn(differentUserDetails)

        // Act & Assert
        assertThrows<AuthenticationServiceException> {
            authenticationService.refreshAccessToken(invalidRefreshToken)
        }
    }
}