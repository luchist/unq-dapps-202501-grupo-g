package com.footballdata.football_stats_predictions.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("e2e")
@Transactional
class HelloControllerE2ETest {

    private val HTTP_LOCALHOST: String = "http://localhost:"

    @LocalServerPort
    private val port = 0

    @Autowired
    private val restTemplate: TestRestTemplate? = null

    @Autowired
    private val webClient: WebTestClient? = null

    @Test
    @Throws(Exception::class)
    fun `should return 401 without authentication`() {
        webClient?.get()?.uri("/api/hello")
        assertThat(
            this.restTemplate?.getForObject(
                "$HTTP_LOCALHOST$port/api/hello",
                String::class.java
            )
        ).contains("error");
    }
}
