package com.footballdata.football_stats_predictions.e2e

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
class HelloControllerE2ETest {

    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `should return 401 without authentication`() {
        val response: ResponseEntity<String> = restTemplate.getForEntity(
            "/api/hello",
            String::class.java
        )

        // Assertions
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        Assertions.assertThat(response.body).contains("error")
        Assertions.assertThat(response.headers.contentType.toString()).contains("application/json")
    }

    @Test
    fun `should return proper error structure for unauthorized access`() {
        val response = restTemplate.getForEntity("/api/hello", Map::class.java)

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        val errorBody = response.body as Map<*, *>

        // Debug
        println("Actual response body: $errorBody")
        println("Keys in response: ${errorBody.keys}")
        println("Response body type: ${errorBody.javaClass}")

        // Assertions
        Assertions.assertThat(errorBody).isNotEmpty

        val responseAsString = errorBody.toString().lowercase()
        Assertions.assertThat(responseAsString).containsAnyOf(
            "unauthorized",
            "access denied",
            "authentication",
            "error"
        )

        // If the fields exist, verify their values
        errorBody["status"]?.let { status ->
            Assertions.assertThat(status).isEqualTo(401)
        }

        errorBody["error"]?.let { error ->
            Assertions.assertThat(error.toString().lowercase()).contains("full authentication is required")
        }
    }

    @Test
    fun `should verify response headers for security`() {
        val response = restTemplate.getForEntity("/api/hello", String::class.java)

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)

        // Security headers assertions
        val headers = response.headers
        Assertions.assertThat(headers.contentType.toString()).contains("application/json")

        Assertions.assertThat(headers.getFirst("Cache-Control")).isNotNull()
        Assertions.assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff")
    }
}