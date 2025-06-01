package com.footballdata.football_stats_predictions.unit.webservice

import com.footballdata.football_stats_predictions.webservice.HelloController
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class HelloControllerTest {
    private val helloController = HelloController()

    @Test
    fun `hello should return ok response with correct message`() {
        // Act
        val response = helloController.hello()

        // Assert
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Hello, Authorized User!", response.body)
    }
}