package com.footballdata.football_stats_predictions.webservice

import com.footballdata.football_stats_predictions.aspects.LogFunctionCall
import com.footballdata.football_stats_predictions.logger
import com.footballdata.football_stats_predictions.service.PlayerService
import com.footballdata.football_stats_predictions.service.QueryHistoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/player")
@Tag(name = "Player", description = "Player endpoints for retrieving player statistics")
class PlayerController(
    @field:Autowired private val playerService: PlayerService,
    @field:Autowired var queryHistoryService: QueryHistoryService
) {
    @Operation(
        summary = "Player endpoint",
        description = "Returns player data for authorized users"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Player stats returned successfully"),
            ApiResponse(responseCode = "403", description = "Access denied for unauthorized users"),
            ApiResponse(responseCode = "404", description = "Player not found")
        ]
    )
    @GetMapping("/{playerName}")
    @LogFunctionCall
    fun getPlayerStats(
        @PathVariable playerName: String,
        authentication: Authentication
    ): ResponseEntity<Any> {
        return executePlayerOperation(
            playerName = playerName,
            endpoint = "/api/player/$playerName",
            authentication = authentication
        ) {
            playerService.getPlayerStats(playerName)
        }
    }

    @GetMapping("/{playerName}/rating")
    @LogFunctionCall
    fun getPlayerRatingsAverage(
        @PathVariable playerName: String,
        authentication: Authentication
    ): ResponseEntity<Any> {
        return executePlayerOperation(
            playerName = playerName,
            endpoint = "/api/player/$playerName/rating",
            authentication = authentication
        ) {
            playerService.getPlayerRatingsAverage(playerName)
        }
    }

    @GetMapping("/{playerName}/compare/{year}")
    @LogFunctionCall
    fun comparePlayerHistory(
        @PathVariable playerName: String,
        @PathVariable year: String,
        authentication: Authentication
    ): ResponseEntity<Any> {
        return executePlayerOperation(
            playerName = playerName,
            endpoint = "/api/player/$playerName/compare/$year",
            authentication = authentication,
            additionalParams = "year=$year"
        ) {
            playerService.comparePlayerHistory(playerName, year)
        }
    }

    /**
     * Utility method to standardize the execution of player-related operations.
     * Encapsulates common logic for exception handling, query logging, and response formatting.
     *
     * @param playerName Name of the player on which the operation is performed
     * @param endpoint Path of the endpoint being executed
     * @param authentication Authentication object of the current user
     * @param additionalParams Optional additional query parameters to log
     * @param operation Lambda function containing the specific operation to execute
     * @return ResponseEntity with the operation result or standardized error message
     */
    private fun <T> executePlayerOperation(
        playerName: String,
        endpoint: String,
        authentication: Authentication,
        additionalParams: String = "",
        operation: () -> T
    ): ResponseEntity<Any> {
        return try {
            val result = operation()
            authentication.let {
                queryHistoryService.saveQuery(
                    userName = it.name,
                    endpoint = endpoint,
                    queryParams = "playerName=$playerName" + if (additionalParams.isNotEmpty()) "&$additionalParams" else "",
                    status = 200
                )
            }
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            val sanitizedMessage = if (e.message.isNullOrEmpty()) {
                "An error occurred while processing the request."
            } else {
                e.message?.replace("[^a-zA-Z0-9 ]".toRegex(), "")?.trim()
            }

            val sanitizedPlayerName = playerName.replace("[^a-zA-Z0-9 ]".toRegex(), "").trim().ifBlank { "Unknown Player" }

            logger.error("Error processing request for player $sanitizedPlayerName: $sanitizedMessage")
            authentication.let {
                queryHistoryService.saveQuery(
                    userName = it.name,
                    endpoint = endpoint,
                    queryParams = "playerName=$playerName" + if (additionalParams.isNotEmpty()) "&$additionalParams" else "",
                    status = 404,
                    message = "Player not found." + e.message
                )
            }
            ResponseEntity.status(404).body(mapOf(
                "message" to "Player not found",
                "error" to 404
            ))
        }
    }
}