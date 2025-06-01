package com.footballdata.football_stats_predictions.e2e

import com.footballdata.football_stats_predictions.dto.AuthenticationRequest
import com.footballdata.football_stats_predictions.dto.AuthenticationResponse
import com.footballdata.football_stats_predictions.model.QueryHistory
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@Transactional
class QueryHistoryControllerE2ETest {

    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `should return 401 without authentication`() {
        // Act
        val response: ResponseEntity<String> = restTemplate.getForEntity(
            "/api/query-history",
            String::class.java
        )

        // Assert
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        Assertions.assertThat(response.body).contains("error")
        Assertions.assertThat(response.headers.contentType.toString()).contains("application/json")
    }

    @Test
    fun `should return empty query history for new authenticated user`() {
        // Arrange
        val uniqueUsername = "testuser_${System.currentTimeMillis()}"
        val authRequest = AuthenticationRequest(
            username = uniqueUsername,
            password = "testpassword123"
        )

        // Register new user (this does not create query history)
        val registerResponse: ResponseEntity<AuthenticationResponse> = restTemplate.postForEntity(
            "/api/auth/register",
            authRequest,
            AuthenticationResponse::class.java
        )

        Assertions.assertThat(registerResponse.statusCode).isEqualTo(HttpStatus.OK)
        val accessToken = registerResponse.body?.accessToken ?: throw IllegalStateException("Access token is null")

        // Act
        val httpHeaders = HttpHeaders()
        httpHeaders.set("Authorization", "Bearer $accessToken")
        val httpEntity = HttpEntity<String>(null, httpHeaders)

        val response: ResponseEntity<List<QueryHistory>> = restTemplate.exchange(
            "/api/query-history",
            HttpMethod.GET,
            httpEntity,
            object : ParameterizedTypeReference<List<QueryHistory>>() {}
        )

        // Assert
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body).isEmpty()
        Assertions.assertThat(response.headers.contentType.toString()).contains("application/json")
    }

    @Test
    fun `should return query history after accessing other endpoints`() {
        // Arrange - Authenticate with existing user
        val authRequest = AuthenticationRequest(
            username = "email-1@gmail.com",
            password = "pass1"
        )

        val authResponse: ResponseEntity<AuthenticationResponse> = restTemplate.postForEntity(
            "/api/auth",
            authRequest,
            AuthenticationResponse::class.java
        )

        Assertions.assertThat(authResponse.statusCode).isEqualTo(HttpStatus.OK)
        val accessToken = authResponse.body?.accessToken ?: throw IllegalStateException("Access token is null")

        val httpHeaders = HttpHeaders()
        httpHeaders.set("Authorization", "Bearer $accessToken")
        val httpEntity = HttpEntity<String>(null, httpHeaders)

        // Act - Access a tracked endpoint (this will create query history)
        restTemplate.exchange(
            "/api/teams/80",
            HttpMethod.GET,
            httpEntity,
            String::class.java
        )

        // Get query history
        val response: ResponseEntity<List<QueryHistory>> = restTemplate.exchange(
            "/api/query-history",
            HttpMethod.GET,
            httpEntity,
            object : ParameterizedTypeReference<List<QueryHistory>>() {}
        )

        // Assert
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body).isNotEmpty
        Assertions.assertThat(response.headers.contentType.toString()).contains("application/json")

        // Verify the query history content
        val queryHistory = response.body!!
        val latestQuery = queryHistory[0] // Should be ordered by timestamp desc

        Assertions.assertThat(latestQuery.userName).isEqualTo("email-1@gmail.com")
        Assertions.assertThat(latestQuery.endpoint).isEqualTo("/api/teams/80")
        Assertions.assertThat(latestQuery.queryParameters).isEqualTo("teamName=80")
        Assertions.assertThat(latestQuery.responseStatus).isEqualTo(200)
        Assertions.assertThat(latestQuery.timestamp).isNotNull
    }

    @Test
    fun `should return query history including failed requests`() {
        // Arrange - Authenticate with existing user
        val authRequest = AuthenticationRequest(
            username = "email-1@gmail.com",
            password = "pass1"
        )

        val authResponse: ResponseEntity<AuthenticationResponse> = restTemplate.postForEntity(
            "/api/auth",
            authRequest,
            AuthenticationResponse::class.java
        )

        Assertions.assertThat(authResponse.statusCode).isEqualTo(HttpStatus.OK)
        val accessToken = authResponse.body?.accessToken ?: throw IllegalStateException("Access token is null")

        val httpHeaders = HttpHeaders()
        httpHeaders.set("Authorization", "Bearer $accessToken")
        val httpEntity = HttpEntity<String>(null, httpHeaders)

        // Act - Access a tracked endpoint that will fail (this will create query history with error)
        restTemplate.exchange(
            "/api/teams/99999",
            HttpMethod.GET,
            httpEntity,
            String::class.java
        )

        // Get query history
        val response: ResponseEntity<List<QueryHistory>> = restTemplate.exchange(
            "/api/query-history",
            HttpMethod.GET,
            httpEntity,
            object : ParameterizedTypeReference<List<QueryHistory>>() {}
        )

        // Assert
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body).isNotEmpty

        // Find the failed request in history
        val queryHistory = response.body!!
        val failedQuery = queryHistory.find { it.responseStatus == 404 }

        Assertions.assertThat(failedQuery).isNotNull
        Assertions.assertThat(failedQuery!!.userName).isEqualTo("email-1@gmail.com")
        Assertions.assertThat(failedQuery.endpoint).isEqualTo("/api/teams/99999")
        Assertions.assertThat(failedQuery.queryParameters).isEqualTo("teamName=99999")
        Assertions.assertThat(failedQuery.responseStatus).isEqualTo(404)
        Assertions.assertThat(failedQuery.responseMessage).contains("Team not found")
    }

    @Test
    fun `should return only user-specific query history`() {
        // Arrange - Create two different users
        val user1Username = "testuser1_${System.currentTimeMillis()}"
        val user2Username = "testuser2_${System.currentTimeMillis()}"

        // Register first user
        val user1AuthRequest = AuthenticationRequest(
            username = user1Username,
            password = "password123"
        )
        val user1RegisterResponse: ResponseEntity<AuthenticationResponse> = restTemplate.postForEntity(
            "/api/auth/register",
            user1AuthRequest,
            AuthenticationResponse::class.java
        )
        val user1AccessToken = user1RegisterResponse.body?.accessToken!!

        // Register second user
        val user2AuthRequest = AuthenticationRequest(
            username = user2Username,
            password = "password123"
        )
        val user2RegisterResponse: ResponseEntity<AuthenticationResponse> = restTemplate.postForEntity(
            "/api/auth/register",
            user2AuthRequest,
            AuthenticationResponse::class.java
        )
        val user2AccessToken = user2RegisterResponse.body?.accessToken!!

        // User 1 makes a request
        val user1Headers = HttpHeaders()
        user1Headers.set("Authorization", "Bearer $user1AccessToken")
        val user1Entity = HttpEntity<String>(null, user1Headers)

        restTemplate.exchange(
            "/api/teams/80",
            HttpMethod.GET,
            user1Entity,
            String::class.java
        )

        // User 2 gets their query history (should be empty)
        val user2Headers = HttpHeaders()
        user2Headers.set("Authorization", "Bearer $user2AccessToken")
        val user2Entity = HttpEntity<String>(null, user2Headers)

        val user2Response: ResponseEntity<List<QueryHistory>> = restTemplate.exchange(
            "/api/query-history",
            HttpMethod.GET,
            user2Entity,
            object : ParameterizedTypeReference<List<QueryHistory>>() {}
        )

        // User 1 gets their query history (should contain their request)
        val user1Response: ResponseEntity<List<QueryHistory>> = restTemplate.exchange(
            "/api/query-history",
            HttpMethod.GET,
            user1Entity,
            object : ParameterizedTypeReference<List<QueryHistory>>() {}
        )

        // Assert
        Assertions.assertThat(user2Response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(user2Response.body).isEmpty() // User 2 should have no history

        Assertions.assertThat(user1Response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(user1Response.body).isNotEmpty // User 1 should have their request
        Assertions.assertThat(user1Response.body!![0].userName).isEqualTo(user1Username)
    }

    @Test
    fun `should verify response headers for security`() {
        // Arrange
        val authRequest = AuthenticationRequest(
            username = "email-1@gmail.com",
            password = "pass1"
        )

        val authResponse: ResponseEntity<AuthenticationResponse> = restTemplate.postForEntity(
            "/api/auth",
            authRequest,
            AuthenticationResponse::class.java
        )

        val accessToken = authResponse.body?.accessToken!!

        val httpHeaders = HttpHeaders()
        httpHeaders.set("Authorization", "Bearer $accessToken")
        val httpEntity = HttpEntity<String>(null, httpHeaders)

        // Act
        val response: ResponseEntity<List<QueryHistory>> = restTemplate.exchange(
            "/api/query-history",
            HttpMethod.GET,
            httpEntity,
            object : ParameterizedTypeReference<List<QueryHistory>>() {}
        )

        // Assert
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        // Security headers assertions
        val headers = response.headers
        Assertions.assertThat(headers.contentType.toString()).contains("application/json")
        Assertions.assertThat(headers.getFirst("Cache-Control")).isNotNull()
        Assertions.assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff")
    }
}
