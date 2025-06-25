@file:Suppress("SpellCheckingInspection", "SpellCheckingInspection")

package com.footballdata.football_stats_predictions.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.footballdata.football_stats_predictions.dto.*
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@Transactional
class AuthControllerE2ETest {

    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should register new user successfully`() {
        // Arrange
        val registerRequest = RegisterRequest(
            username = "test_user_${System.currentTimeMillis()}",
            password = "test_password123"
        )

        // Act
        val response: ResponseEntity<AuthenticationResponse> = restTemplate.postForEntity(
            "/api/auth/register",
            registerRequest,
            AuthenticationResponse::class.java
        )

        // Assert
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body?.accessToken).isNotBlank
        Assertions.assertThat(response.body?.refreshToken).isNotBlank
        Assertions.assertThat(response.headers.contentType.toString()).contains("application/json")
    }

    @Test
    fun `should return 400 when registering user with blank username`() {
        // Arrange
        val registerRequest = RegisterRequest(
            username = "",
            password = "test_password123"
        )

        // Act
        val response: ResponseEntity<Map<*, *>> = restTemplate.postForEntity(
            "/api/auth/register",
            registerRequest,
            Map::class.java
        )

        // Assert
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        val errorBody = response.body as Map<*, *>

        Assertions.assertThat(errorBody["status"]).isEqualTo(400)
        Assertions.assertThat(errorBody["error"]).isEqualTo("Bad Request")
        Assertions.assertThat(errorBody["message"]).isEqualTo("Cannot pass null or empty values to constructor")
    }

    @Test
    fun `should authenticate existing user successfully`() {
        // Arrange
        val username = "auth_user_${System.currentTimeMillis()}"
        val password = "auth_password123"

        val registerRequest = RegisterRequest(username = username, password = password)
        restTemplate.postForEntity("/api/auth/register", registerRequest, AuthenticationResponse::class.java)

        val authRequest = AuthenticationRequest(username = username, password = password)

        // Act
        val response: ResponseEntity<AuthenticationResponse> = restTemplate.postForEntity(
            "/api/auth",
            authRequest,
            AuthenticationResponse::class.java
        )

        // Assert
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body?.accessToken).isNotBlank
        Assertions.assertThat(response.body?.refreshToken).isNotBlank
    }

    @Test
    fun `should return 401 for invalid authentication credentials`() {
        // Arrange
        val authRequest = AuthenticationRequest(
            username = "wrong_user",
            password = "wrong_password"
        )

        // Act
        val response: ResponseEntity<Map<*, *>> = restTemplate.postForEntity(
            "/api/auth",
            authRequest,
            Map::class.java
        )

        // Assert
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        val errorBody = response.body as Map<*, *>

        val responseAsString = errorBody.toString().lowercase()
        Assertions.assertThat(responseAsString).containsAnyOf(
            "forbidden",
            "unauthorized",
            "authentication",
            "invalid",
            "error"
        )
    }

    @Test
    fun `should refresh access token successfully`() {
        // Arrange - First register and authenticate a user
        val username = "refresh_user_${System.currentTimeMillis()}"
        val password = "refresh_password123"

        val registerRequest = RegisterRequest(username = username, password = password)
        val authResponse = restTemplate.postForEntity(
            "/api/auth/register",
            registerRequest,
            AuthenticationResponse::class.java
        )

        val refreshToken = authResponse.body?.refreshToken!!
        val refreshRequest = RefreshTokenRequest(token = refreshToken)

        // Act
        val response: ResponseEntity<TokenResponse> = restTemplate.postForEntity(
            "/api/auth/refresh",
            refreshRequest,
            TokenResponse::class.java
        )

        // Assert
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body?.token).isNotBlank
        Assertions.assertThat(response.body?.error).isNull()
    }

    @Test
    fun `should return 401 for invalid refresh token`() {
        // Arrange
        val refreshRequest = RefreshTokenRequest(token = "invalid_refresh_token")

        // Act
        val response: ResponseEntity<TokenResponse> = restTemplate.postForEntity(
            "/api/auth/refresh",
            refreshRequest,
            TokenResponse::class.java
        )

        // Assert
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body?.token).isNull()
        Assertions.assertThat(response.body?.error).isNotNull
        Assertions.assertThat(response.body?.error).containsAnyOf("Invalid", "expired", "token")
    }

    @Test
    fun `should return 400 for malformed authentication request`() {
        // Arrange - Send request with missing required fields
        val malformedRequest = AuthenticationRequest(
            username = "", // This should trigger validation error
            password = ""  // This should trigger validation error
        )

        // Act
        val response: ResponseEntity<Map<*, *>> = restTemplate.postForEntity(
            "/api/auth",
            malformedRequest,
            Map::class.java
        )

        // Assert
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        val errorBody = response.body as Map<*, *>

        Assertions.assertThat(errorBody["status"]).isEqualTo(400)
        Assertions.assertThat(errorBody["error"]).isEqualTo("Bad Request")
        Assertions.assertThat(errorBody["message"]).isEqualTo("Cannot pass null or empty values to constructor")
    }

    @Test
    fun `should verify response headers for authentication endpoints`() {
        // Arrange
        val username = "header_user_${System.currentTimeMillis()}"
        val password = "header_password123"
        val registerRequest = RegisterRequest(username = username, password = password)

        // Act
        val response = restTemplate.postForEntity(
            "/api/auth/register",
            registerRequest,
            AuthenticationResponse::class.java
        )

        // Assert
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        // Security headers assertions
        val headers = response.headers
        Assertions.assertThat(headers.contentType.toString()).contains("application/json")
    }
}
