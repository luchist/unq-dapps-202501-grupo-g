package com.footballdata.football_stats_predictions.unittest.service

import com.footballdata.football_stats_predictions.model.QueryHistory
import com.footballdata.football_stats_predictions.repositories.QueryHistoryRepository
import com.footballdata.football_stats_predictions.service.QueryHistoryService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class QueryHistoryServiceTest {

    @Mock
    private lateinit var queryHistoryRepository: QueryHistoryRepository

    @InjectMocks
    private lateinit var queryHistoryService: QueryHistoryService

    @Test
    fun `saveQuery should save query history successfully`() {
        // Arrange
        val userName = "testUser"
        val endpoint = "/api/teams/Barcelona"
        val queryParams = "teamName=Barcelona"
        val status = 200
        val message = "Success"

        val expectedQueryHistory = QueryHistory(
            id = 1L,
            userName = userName,
            endpoint = endpoint,
            queryParameters = queryParams,
            responseStatus = status,
            responseMessage = message
        )

        `when`(queryHistoryRepository.save(ArgumentMatchers.any(QueryHistory::class.java)))
            .thenReturn(expectedQueryHistory)

        // Act
        val result = queryHistoryService.saveQuery(userName, endpoint, queryParams, status, message)

        // Assert
        assertEquals(expectedQueryHistory, result)
        verify(queryHistoryRepository).save(ArgumentMatchers.any(QueryHistory::class.java))
    }

    @Test
    fun `saveQuery should save query history without message`() {
        // Arrange
        val userName = "testUser"
        val endpoint = "/api/teams/Madrid"
        val queryParams = "teamName=Madrid"
        val status = 404

        val expectedQueryHistory = QueryHistory(
            id = 2L,
            userName = userName,
            endpoint = endpoint,
            queryParameters = queryParams,
            responseStatus = status,
            responseMessage = null
        )

        `when`(queryHistoryRepository.save(ArgumentMatchers.any(QueryHistory::class.java)))
            .thenReturn(expectedQueryHistory)

        // Act
        val result = queryHistoryService.saveQuery(userName, endpoint, queryParams, status)

        // Assert
        assertEquals(expectedQueryHistory, result)
        verify(queryHistoryRepository).save(ArgumentMatchers.any(QueryHistory::class.java))
    }

    @Test
    fun `getUserQueryHistory should return user history ordered by timestamp`() {
        // Arrange
        val userName = "testUser"
        val expectedHistory = listOf(
            QueryHistory(
                1L, userName, "/api/teams/Barcelona", "teamName=Barcelona",
                LocalDateTime.now(), 200
            ),
            QueryHistory(
                2L, userName, "/api/teams/Madrid", "teamName=Madrid",
                LocalDateTime.now().minusHours(1), 404
            )
        )

        `when`(queryHistoryRepository.findByUserNameOrderByTimestampDesc(userName))
            .thenReturn(expectedHistory)

        // Act
        val result = queryHistoryService.getUserQueryHistory(userName)

        // Assert
        assertEquals(expectedHistory, result)
        verify(queryHistoryRepository).findByUserNameOrderByTimestampDesc(userName)
    }


    @Test
    fun `getUserQueryHistory should return empty list for user with no history`() {
        // Arrange
        val userName = "testUser"
        val emptyHistory = emptyList<QueryHistory>()

        `when`(queryHistoryRepository.findByUserNameOrderByTimestampDesc(userName))
            .thenReturn(emptyHistory)

        // Act
        val result = queryHistoryService.getUserQueryHistory(userName)

        // Assert
        assertEquals(emptyHistory, result)
        verify(queryHistoryRepository).findByUserNameOrderByTimestampDesc(userName)
    }
}
