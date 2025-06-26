package com.footballdata.football_stats_predictions.unit.service

import com.footballdata.football_stats_predictions.dto.AuthenticationRequest
import com.footballdata.football_stats_predictions.repositories.RefreshTokenRepository
import com.footballdata.football_stats_predictions.service.AuthenticationService
import com.footballdata.football_stats_predictions.service.TokenService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import java.util.*

class AuthenticationServiceTest {
    private lateinit var authenticationService: AuthenticationService
    private lateinit var authManager: AuthenticationManager
    private lateinit var userDetailsService: UserDetailsService
    private lateinit var tokenService: TokenService
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    private val accessTokenExpiration = 300000L // 5 minutes
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

    @Test
    fun `authentication should return valid tokens when credentials are correct`() {
        // Arrange
        val authRequest = AuthenticationRequest(testUsername, testPassword)
        val mockUserDetails = mock(UserDetails::class.java).apply {
            `when`(username).thenReturn(testUsername)
        }
        val expectedAccessToken = "valid.access.token"
        val expectedRefreshToken = "valid.refresh.token"
        val mockAuthentication = mock(UsernamePasswordAuthenticationToken::class.java)

        `when`(authManager.authenticate(any<UsernamePasswordAuthenticationToken>()))
            .thenReturn(mockAuthentication)
        `when`(userDetailsService.loadUserByUsername(testUsername))
            .thenReturn(mockUserDetails)
        `when`(
            tokenService.generateToken
                (
                any<String>(),
                any<Date>(),
                any<Map<String, Any>>()
            )
        )
            .thenReturn(expectedAccessToken)
            .thenReturn(expectedRefreshToken)

        // Act
        val result = authenticationService.authentication(authRequest)

        // Assert
        assertNotNull(result)
        assertEquals(expectedAccessToken, result.accessToken)
        assertEquals(expectedRefreshToken, result.refreshToken)
        verify(authManager).authenticate(any<UsernamePasswordAuthenticationToken>())
        verify(userDetailsService).loadUserByUsername(testUsername)
        verify(tokenService, times(2)).generateToken(any<String>(), any<Date>(), any<Map<String, Any>>())
        verify(refreshTokenRepository).save(expectedRefreshToken, mockUserDetails)
    }

    @Test
    fun `refreshAccessToken should return new access token when refresh token is valid`() {
        // Arrange
        val validRefreshToken = "valid.refresh.token"
        val newAccessToken = "new.access.token"
        val mockUserDetails = mock(UserDetails::class.java).apply {
            `when`(username).thenReturn(testUsername)
        }

        `when`(tokenService.extractUsername(validRefreshToken))
            .thenReturn(testUsername)
        `when`(userDetailsService.loadUserByUsername(testUsername))
            .thenReturn(mockUserDetails)
        `when`(refreshTokenRepository.findUserDetailsByToken(validRefreshToken))
            .thenReturn(mockUserDetails)
        `when`(
            tokenService.generateToken(
                any<String>(),
                any<Date>(),
                any<Map<String, String>>()
            )
        )
            .thenReturn(newAccessToken)

        // Act
        val result = authenticationService.refreshAccessToken(validRefreshToken)

        // Assert
        assertEquals(newAccessToken, result)
        verify(tokenService).extractUsername(validRefreshToken)
        verify(userDetailsService).loadUserByUsername(testUsername)
        verify(refreshTokenRepository).findUserDetailsByToken(validRefreshToken)
        verify(tokenService).generateToken(any<String>(), any<Date>(), any<Map<String, String>>())
    }

    @Test
    fun `authentication should call authentication manager with correct credentials`() {
        // Arrange
        val authRequest = AuthenticationRequest(testUsername, testPassword)
        val mockUserDetails = mock(UserDetails::class.java).apply {
            `when`(username).thenReturn(testUsername)
        }
        val mockAuthentication = mock(UsernamePasswordAuthenticationToken::class.java)

        `when`(authManager.authenticate(any<UsernamePasswordAuthenticationToken>()))
            .thenReturn(mockAuthentication)
        `when`(userDetailsService.loadUserByUsername(testUsername))
            .thenReturn(mockUserDetails)
        `when`(
            tokenService.generateToken(
                any<String>(),
                any<Date>(),
                any<Map<String, Any>>()
            )
        )
            .thenReturn("access.token")
            .thenReturn("refresh.token")

        // Act
        authenticationService.authentication(authRequest)

        // Assert
        verify(authManager).authenticate(argThat { authToken ->
            authToken.principal == testUsername &&
                    authToken.credentials == testPassword
        })
    }

    @Test
    fun `authentication should save refresh token with correct user details`() {
        // Arrange
        val authRequest = AuthenticationRequest(testUsername, testPassword)
        val mockUserDetails = mock(UserDetails::class.java).apply {
            `when`(username).thenReturn(testUsername)
        }
        val expectedRefreshToken = "refresh.token.value"
        val mockAuthentication = mock(UsernamePasswordAuthenticationToken::class.java)

        `when`(authManager.authenticate(any<UsernamePasswordAuthenticationToken>()))
            .thenReturn(mockAuthentication)
        `when`(userDetailsService.loadUserByUsername(testUsername))
            .thenReturn(mockUserDetails)
        `when`(
            tokenService.generateToken(
                any<String>(),
                any<Date>(),
                any<Map<String, Any>>()
            )
        )
            .thenReturn("access.token")
            .thenReturn(expectedRefreshToken)

        // Act
        authenticationService.authentication(authRequest)

        // Assert
        verify(refreshTokenRepository).save(expectedRefreshToken, mockUserDetails)
    }
}