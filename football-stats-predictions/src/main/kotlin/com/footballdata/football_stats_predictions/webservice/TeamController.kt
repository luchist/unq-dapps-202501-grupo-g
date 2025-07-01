package com.footballdata.football_stats_predictions.webservice

import com.footballdata.football_stats_predictions.aspects.LogFunctionCall
import com.footballdata.football_stats_predictions.logger
import com.footballdata.football_stats_predictions.model.TeamStats
import com.footballdata.football_stats_predictions.service.QueryHistoryService
import com.footballdata.football_stats_predictions.service.TeamService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
@RequestMapping("/api/team")
@Tag(name = "Team", description = "Endpoints for team-related operations")
class TeamController(
    @field:Autowired var teamService: TeamService,
    @field:Autowired var queryHistoryService: QueryHistoryService
) {
    @Operation(summary = "Get all team members", description = "Returns a list of Players of a Team")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "List of players returned successfully"),
            ApiResponse(responseCode = "404", description = "Team not found")
        ]
    )
    @GetMapping("/{teamName}")
    @LogFunctionCall
    fun getTeamComposition(
        @Parameter(description = "The team name that needs to be fetched", required = true)
        @PathVariable teamName: String,
        authentication: Authentication
    ): ResponseEntity<Any> {
        return executeTeamOperation(
            teamName = teamName,
            endpoint = "/api/team/$teamName",
            authentication = authentication
        ) {
            teamService.getTeamComposition(teamName)
        }
    }

    @Operation(summary = "Get scheduled matches", description = "Returns a list of scheduled Matches for a team")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "List of scheduled matches returned successfully"),
            ApiResponse(responseCode = "404", description = "Team not found")
        ]
    )
    @GetMapping("/{teamName}/matches")
    @LogFunctionCall
    fun getScheduledMatches(
        @Parameter(
            description = "The team name for which scheduled matches are needed",
            required = true
        )
        @PathVariable teamName: String,
        authentication: Authentication
    ): ResponseEntity<Any> {
        return executeTeamOperation(
            teamName = teamName,
            endpoint = "/api/team/$teamName/matches",
            authentication = authentication
        ) {
            teamService.getScheduledMatches(teamName)
        }
    }

    @Operation(summary = "Get team statistics", description = "Returns a list of statistics of a Team")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "List of statistics returned successfully"),
            ApiResponse(responseCode = "403", description = "Access denied for unauthorized users"),
            ApiResponse(responseCode = "404", description = "Team not found")
        ]
    )
    @GetMapping("/stats/{teamName}")
    @LogFunctionCall
    fun getTeamStats(
        @PathVariable teamName: String,
        authentication: Authentication
    ): ResponseEntity<Any> {
        return executeTeamOperation(
            teamName = teamName,
            endpoint = "/api/team/stats/$teamName",
            authentication = authentication
        ) {
            teamService.getTeamStatistics(teamName)
        }
    }

    @Operation(
        summary = "Get Team advanced statistics",
        description = "Returns a list of advanced statistics of a Team"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "List of advanced statistics returned successfully"),
            ApiResponse(responseCode = "403", description = "Access denied for unauthorized users"),
            ApiResponse(responseCode = "404", description = "Team was not found")
        ]
    )
    @GetMapping("/advanced/{teamName}")
    @LogFunctionCall
    fun getTeamAdvancedStatistics(
        @PathVariable teamName: String,
        authentication: Authentication
    ): ResponseEntity<Any> {
        return executeTeamOperation(
            teamName = teamName,
            endpoint = "/api/team/advanced/$teamName",
            authentication = authentication
        ) {
            teamService.getTeamAdvancedStatistics(teamName)
        }
    }

    @Operation(summary = "Get prediction match between two teams", description = "Returns a list of predictions")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "List of predictions returned successfully"),
            ApiResponse(responseCode = "403", description = "Access denied for unauthorized users"),
            ApiResponse(responseCode = "404", description = "One or more teams was not found")
        ]
    )
    @GetMapping("/predict/{localTeam}/{awayTeam}")
    @LogFunctionCall
    fun predictMatchProbabilities(
        @PathVariable localTeam: String,
        @PathVariable awayTeam: String,
        authentication: Authentication
    ): ResponseEntity<Any> {
        return executeTeamsOperation(
            team1 = localTeam,
            team2 = awayTeam,
            endpoint = "/api/team/predict/$localTeam/$awayTeam",
            authentication = authentication
        ) {
            teamService.predictMatchProbabilities(localTeam, awayTeam)
        }
    }


    @Operation(summary = "Get comparison between two teams", description = "Returns a comparison of two teams")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Comparison returned successfully"),
            ApiResponse(responseCode = "403", description = "Access denied for unauthorized users"),
            ApiResponse(responseCode = "404", description = "One or more teams was not found")
        ]
    )
    @GetMapping("/compare/{localTeam}/{awayTeam}")
    @LogFunctionCall
    fun compareTeams(
        @PathVariable localTeam: String,
        @PathVariable awayTeam: String,
        authentication: Authentication
    ): ResponseEntity<Any> {
        return executeTeamsOperation(
            team1 = localTeam,
            team2 = awayTeam,
            endpoint = "/api/team/compare/$localTeam/$awayTeam",
            authentication = authentication
        ) {
            teamService.compareTeams(localTeam, awayTeam)
        }
    }

    /**
     * Utility method to standardize the execution of team-related operations.
     * Encapsulates common logic for exception handling, query logging, and response formatting.
     *
     * @param teamName Name of the team on which the operation is performed
     * @param endpoint Path of the endpoint being executed
     * @param authentication Authentication object of the current user
     * @param operation Lambda function containing the specific operation to execute
     * @return ResponseEntity with the operation result or standardized error message
     */
    private fun <T> executeTeamOperation(
        teamName: String,
        endpoint: String,
        authentication: Authentication,
        operation: () -> T
    ): ResponseEntity<Any> {
        return try {
            val result = operation()
            authentication.let {
                queryHistoryService.saveQuery(
                    userName = it.name,
                    endpoint = endpoint,
                    queryParams = "teamName=$teamName",
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

            val sanitizedTeamName = teamName.replace("[^a-zA-Z0-9 ]".toRegex(), "").trim().ifBlank { "Unknown Team" }

            logger.error("Error processing request for $sanitizedTeamName: $sanitizedMessage")
            authentication.let {
                queryHistoryService.saveQuery(
                    userName = it.name,
                    endpoint = endpoint,
                    queryParams = "teamName=$teamName",
                    status = 404,
                    message = "Team not found." + e.message
                )
            }
            ResponseEntity.status(404).body(mapOf(
                "message" to "Team not found",
                "error" to 404
            ))
        }
    }

    /**
     * Utility method to standardize the execution of operations involving two teams.
     * Encapsulates common logic for exception handling, query logging, and response formatting.
     *
     * @param team1 Name of the first team
     * @param team2 Name of the second team
     * @param endpoint Path of the endpoint being executed
     * @param authentication Authentication object of the current user
     * @param operation Lambda function containing the specific operation to execute
     * @return ResponseEntity with the operation result or standardized error message
     */
    private fun <T> executeTeamsOperation(
        team1: String,
        team2: String,
        endpoint: String,
        authentication: Authentication,
        operation: () -> T
    ): ResponseEntity<Any> {
        return try {
            val result = operation()
            authentication.let {
                queryHistoryService.saveQuery(
                    userName = it.name,
                    endpoint = endpoint,
                    queryParams = "team1=$team1&team2=$team2",
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

            val sanitizedTeam1 = team1.replace("[^a-zA-Z0-9 ]".toRegex(), "").trim().ifBlank { "Unknown Team" }
            val sanitizedTeam2 = team2.replace("[^a-zA-Z0-9 ]".toRegex(), "").trim().ifBlank { "Unknown Team" }

            logger.error("Error processing request for teams $sanitizedTeam1 and $sanitizedTeam2: $sanitizedMessage")
            authentication.let {
                queryHistoryService.saveQuery(
                    userName = it.name,
                    endpoint = endpoint,
                    queryParams = "team1=$team1&team2=$team2",
                    status = 404,
                    message = "One or more teams not found." + e.message
                )
            }
            ResponseEntity.status(404).body(mapOf(
                "message" to "One or more teams not found",
                "error" to 404
            ))
        }
    }
}