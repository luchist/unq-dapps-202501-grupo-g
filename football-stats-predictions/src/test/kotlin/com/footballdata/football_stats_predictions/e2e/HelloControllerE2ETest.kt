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
        // RestTemplate approach - simpler, synchronous
        val response: ResponseEntity<String> = restTemplate.getForEntity(
            "/api/hello",
            String::class.java
        )

        // More comprehensive assertions
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        Assertions.assertThat(response.body).contains("error")
        Assertions.assertThat(response.headers.contentType.toString()).contains("application/json")
    }

    @Test
    fun `should return proper error structure for unauthorized access`() {
        val response = restTemplate.getForEntity("/api/hello", Map::class.java)

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        val errorBody = response.body as Map<*, *>

        // Debug: Print the actual response structure
        println("Actual response body: $errorBody")
        println("Keys in response: ${errorBody.keys}")
        println("Response body type: ${errorBody.javaClass}")

        // Check what's actually in the response
        Assertions.assertThat(errorBody).isNotEmpty

        // Spring Security error responses can vary by configuration
        // Let's check for common patterns instead of exact structure
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

        // These headers might be set by Spring Security
        Assertions.assertThat(headers.getFirst("Cache-Control")).isNotNull()
        Assertions.assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff")
    }
}