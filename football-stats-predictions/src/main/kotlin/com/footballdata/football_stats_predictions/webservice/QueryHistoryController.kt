package com.footballdata.football_stats_predictions.webservice

import com.footballdata.football_stats_predictions.model.QueryHistory
import com.footballdata.football_stats_predictions.service.QueryHistoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/query-history")
@Tag(name = "Query History", description = "Endpoints for query history operations")
class QueryHistoryController(@Autowired private val queryHistoryService: QueryHistoryService) {

    @Operation(summary = "Get user query history", description = "Returns query history for authenticated user")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Query history retrieved successfully"),
            ApiResponse(responseCode = "401", description = "User not authenticated")
        ]
    )
    @GetMapping
    fun getUserQueryHistory(authentication: Authentication): List<QueryHistory> {
        val userName = authentication.name
        return queryHistoryService.getUserQueryHistory(userName)
    }
}
