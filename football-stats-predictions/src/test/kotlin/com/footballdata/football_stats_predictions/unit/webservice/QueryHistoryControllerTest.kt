package com.footballdata.football_stats_predictions.unit.webservice

import com.footballdata.football_stats_predictions.model.QueryHistory
import com.footballdata.football_stats_predictions.service.QueryHistoryService
import com.footballdata.football_stats_predictions.webservice.QueryHistoryController
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.core.Authentication
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class QueryHistoryControllerTest {

    @Mock
    private lateinit var queryHistoryService: QueryHistoryService

    @Mock
    private lateinit var authentication: Authentication

    @InjectMocks
    private lateinit var queryHistoryController: QueryHistoryController

    @Test
    fun `getUserQueryHistory should return user history`() {
        // Arrange
        val userName = "TestUser"
        val expectedHistory = listOf(
            QueryHistory(1L, userName, "/api/teams/Barcelona", "teamName=Barcelona", LocalDateTime.now(), 200),
            QueryHistory(2L, userName, "/api/teams/Madrid", "teamName=Madrid", LocalDateTime.now().minusHours(1), 404)
        )

        `when`(authentication.name).thenReturn(userName)
        `when`(queryHistoryService.getUserQueryHistory(userName)).thenReturn(expectedHistory)

        // Act
        val result = queryHistoryController.getUserQueryHistory(authentication)

        // Assert
        assertEquals(expectedHistory, result)
        verify(queryHistoryService).getUserQueryHistory(userName)
    }

    @Test
    fun `getUserQueryHistory should return empty list for user with no history`() {
        // Arrange
        val userName = "TestUser"
        val emptyHistory = emptyList<QueryHistory>()

        `when`(authentication.name).thenReturn(userName)
        `when`(queryHistoryService.getUserQueryHistory(userName)).thenReturn(emptyHistory)

        // Act
        val result = queryHistoryController.getUserQueryHistory(authentication)

        // Assert
        assertEquals(emptyHistory, result)
        verify(queryHistoryService).getUserQueryHistory(userName)
    }
}
