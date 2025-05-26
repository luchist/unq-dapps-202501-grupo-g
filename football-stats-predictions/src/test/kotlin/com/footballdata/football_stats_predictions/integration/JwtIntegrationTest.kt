package com.footballdata.football_stats_predictions.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.footballdata.football_stats_predictions.FootballStatsPredictionsApplication
import com.footballdata.football_stats_predictions.dto.AuthenticationRequest
import com.footballdata.football_stats_predictions.dto.AuthenticationResponse
import com.footballdata.football_stats_predictions.dto.RefreshTokenRequest
import com.footballdata.football_stats_predictions.dto.TokenResponse
import com.footballdata.football_stats_predictions.service.TokenService
import io.jsonwebtoken.ExpiredJwtException
import org.junit.jupiter.api.Test
import org.mockito.Mockito.reset
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest(classes = [FootballStatsPredictionsApplication::class])
@AutoConfigureMockMvc
@TestPropertySource(
    properties = [
        "jwt.secret=25cf3c44c8f39313e8cbf7c23e22fe8b2ee8b288ee5206b0a6397583a1f7f0ef",
        "jwt.accessTokenExpiration=60000",
        "jwt.refreshTokenExpiration=360000",
        "jwt.expiredToken=eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJlbWFpbC0xQGdtYWlsLmNvbSIsImlhdCI6MTcyNzQ1Mjk2MCwiZXhwIjoxNzI3NDU2NTYwfQ.oP0dNWn75v8Ka7fvxt-966ug2q3A5i4Ef-urjo0bQtSCZeq9f4ijA7HydBC-xMX2"
    ]
)
class JwtIntegrationTest {
    @Value("\${jwt.expiredToken}")
    private lateinit var expiredToken: String

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoSpyBean
    private lateinit var tokenService: TokenService

    @MockitoSpyBean
    private lateinit var userDetailsService: UserDetailsService

    @Test
    fun `access secured endpoint with new token from the refresh token after token expiration`() {
        val authRequest = AuthenticationRequest("email-1@gmail.com", "pass1")
        var jsonRequest = jacksonObjectMapper().writeValueAsString(authRequest)

        var response = mockMvc.perform(
            post("/api/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty).andReturn().response.contentAsString

        val authResponse = jacksonObjectMapper().readValue(response, AuthenticationResponse::class.java)

        // access secured endpoint
        mockMvc.perform(
            get("/api/hello")
                .header("Authorization", "Bearer ${authResponse.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(content().string("Hello, Authorized User!"))

        // simulate access token expiration
        `when`(tokenService.extractUsername(authResponse.accessToken))
            .thenThrow(ExpiredJwtException::class.java)

        mockMvc.perform(
            get("/api/hello")
                .header("Authorization", "Bearer ${authResponse.accessToken}")
        )
            .andExpect(status().isUnauthorized)

        // create a new access token from the refresh token
        val refreshTokenRequest = RefreshTokenRequest(authResponse.refreshToken)
        jsonRequest = jacksonObjectMapper().writeValueAsString(refreshTokenRequest)

        response = mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").isNotEmpty).andReturn().response.contentAsString

        val newAccessToken = jacksonObjectMapper().readValue(response, TokenResponse::class.java)

        reset(tokenService)

        // access secured endpoint with the new token
        mockMvc.perform(
            get("/api/hello")
                .header("Authorization", "Bearer ${newAccessToken.token}")
        )
            .andExpect(status().isOk)
            .andExpect(content().string("Hello, Authorized User!"))
    }

    @Test
    fun `refresh token with invalid refresh token should return unauthorized`() {
        val jsonRequest = jacksonObjectMapper().writeValueAsString(
            RefreshTokenRequest(
                expiredToken
            )
        )

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return unauthorized for unauthenticated user`() {
        val authRequest = AuthenticationRequest("some-user", "pass1")
        val jsonRequest = jacksonObjectMapper().writeValueAsString(authRequest)

        mockMvc.perform(
            post("/api/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return unauthorized for tampered refresh token`() {
        val authRequest = AuthenticationRequest("email-1@gmail.com", "pass1")
        var jsonRequest = jacksonObjectMapper().writeValueAsString(authRequest)

        val response = mockMvc.perform(
            post("/api/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty).andReturn().response.contentAsString

        val authResponse = jacksonObjectMapper().readValue(response, AuthenticationResponse::class.java)

        val refreshTokenRequest = RefreshTokenRequest(authResponse.refreshToken)
        jsonRequest = jacksonObjectMapper().writeValueAsString(refreshTokenRequest)

        `when`(userDetailsService.loadUserByUsername("email-1@gmail.com"))
            .thenReturn(User("email-2@gmail.com", "pass2", ArrayList()))

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return unauthorized for tampered token`() {
        val authRequest = AuthenticationRequest("email-1@gmail.com", "pass1")
        val jsonRequest = jacksonObjectMapper().writeValueAsString(authRequest)

        val response = mockMvc.perform(
            post("/api/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty).andReturn().response.contentAsString

        val authResponse = jacksonObjectMapper().readValue(response, AuthenticationResponse::class.java)

        `when`(userDetailsService.loadUserByUsername("email-1@gmail.com"))
            .thenReturn(User("email-2@gmail.com", "pass2", ArrayList()))

        mockMvc.perform(
            get("/api/hello")
                .header("Authorization", "Bearer ${authResponse.accessToken}")
        )
            .andExpect(status().isUnauthorized)
    }
}

