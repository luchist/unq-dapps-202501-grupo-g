package com.footballdata.football_stats_predictions.e2e

import com.footballdata.football_stats_predictions.dto.AuthenticationRequest
import com.footballdata.football_stats_predictions.dto.AuthenticationResponse
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
class TeamControllerE2ETest {
    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `should return 401 without authentication and valid team`() {
        val response: ResponseEntity<String> = restTemplate.getForEntity(
            "/api/team/1769",
            String::class.java
        )

        // Assertions
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        Assertions.assertThat(response.body).contains("error")
        Assertions.assertThat(response.headers.contentType.toString()).contains("application/json")
    }

    @Test
    fun `should return 200 with authentication and valid team`() {

        //login first using existing user credentials
        val registerRequest = AuthenticationRequest(
            username = "email-1@gmail.com",
            password = "pass1"
        )

        val authResponse: ResponseEntity<AuthenticationResponse> = restTemplate.postForEntity(
            "/api/auth",
            registerRequest,
            AuthenticationResponse::class.java
        )

        Assertions.assertThat(authResponse.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(authResponse.body).isNotNull
        Assertions.assertThat(authResponse.body?.accessToken).isNotBlank
        Assertions.assertThat(authResponse.body?.refreshToken).isNotBlank
        Assertions.assertThat(authResponse.headers.contentType.toString()).contains("application/json")

        // Use the access token to authenticate the request
        val accessToken = authResponse.body?.accessToken ?: throw IllegalStateException("Access token is null")

        val httpHeaders = HttpHeaders()
        httpHeaders.set("Authorization", "Bearer $accessToken")
        val httpEntity = HttpEntity<String>(null, httpHeaders)

        val response: ResponseEntity<String> = restTemplate.exchange(
            "/api/team/1769",
            org.springframework.http.HttpMethod.GET,
            httpEntity,
            String::class.java
        )


        // Assertions
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotBlank
        Assertions.assertThat(response.headers.contentType.toString()).contains("application/json")
        Assertions.assertThat(response.body).contains("playerName")
    }

    @Test
    fun `should return 404 with authentication and non-existing team`() {
        //login first using existing user credentials
        val registerRequest = AuthenticationRequest(
            username = "email-1@gmail.com",
            password = "pass1"
        )

        val authResponse: ResponseEntity<AuthenticationResponse> = restTemplate.postForEntity(
            "/api/auth",
            registerRequest,
            AuthenticationResponse::class.java
        )

        Assertions.assertThat(authResponse.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(authResponse.body).isNotNull
        Assertions.assertThat(authResponse.body?.accessToken).isNotBlank
        Assertions.assertThat(authResponse.body?.refreshToken).isNotBlank
        Assertions.assertThat(authResponse.headers.contentType.toString()).contains("application/json")

        // Use the access token to authenticate the request
        val accessToken = authResponse.body?.accessToken ?: throw IllegalStateException("Access token is null")

        val httpHeaders = HttpHeaders()
        httpHeaders.set("Authorization", "Bearer $accessToken")
        val httpEntity = HttpEntity<String>(null, httpHeaders)

        val response: ResponseEntity<String> = restTemplate.exchange(
            "/api/team/99999",
            org.springframework.http.HttpMethod.GET,
            httpEntity,
            String::class.java
        )

        // Assertions
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        Assertions.assertThat(response.headers.contentType.toString()).contains("application/json")
        Assertions.assertThat(response.body).isNotBlank
        Assertions.assertThat(response.body).contains("Team not found")
        Assertions.assertThat(response.body).contains("404")
    }
}