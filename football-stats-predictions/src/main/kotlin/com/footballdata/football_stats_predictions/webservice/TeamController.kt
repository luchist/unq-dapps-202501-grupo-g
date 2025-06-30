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
        @Parameter(
            description = "The team name that needs to be fetched",
            required = true
        )
        @PathVariable teamName: String,
        authentication: Authentication
    ): ResponseEntity<Any> {
        return try {
            val players = teamService.getTeamComposition(teamName)
            authentication.let {
                queryHistoryService.saveQuery(
                    userName = it.name,
                    endpoint = "/api/team/$teamName",
                    queryParams = "teamName=$teamName",
                    status = 200
                )
            }
            ResponseEntity.ok(players)
        } catch (e: Exception) {
            logger.error("Error fetching team composition for $teamName: ${e.message}")
            authentication.let {
                queryHistoryService.saveQuery(
                    userName = it.name,
                    endpoint = "/api/team/$teamName",
                    queryParams = "teamName=$teamName",
                    status = 404,
                    message = "Team not found." + e.message
                )
            }
            val errorBody = mapOf(
                "message" to ("Team not found"),
                "error" to 404
            )
            ResponseEntity.status(404).body(errorBody)
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
        return try {
            val matches = teamService.getScheduledMatches(teamName)
            authentication.let {
                queryHistoryService.saveQuery(
                    userName = it.name,
                    endpoint = "/api/team/$teamName/matches",
                    queryParams = "teamName=$teamName",
                    status = 200
                )
            }
            ResponseEntity.ok(matches)
        } catch (e: Exception) {
            logger.error("Error fetching scheduled Matches for $teamName: ${e.message}")
            authentication.let {
                queryHistoryService.saveQuery(
                    userName = it.name,
                    endpoint = "/api/team/$teamName/matches",
                    queryParams = "teamName=$teamName",
                    status = 404,
                    message = "Team not found." + e.message
                )
            }
            val errorBody = mapOf(
                "message" to ("Team not found"),
                "error" to 404
            )
            ResponseEntity.status(404).body(errorBody)
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
    fun getTeamStats(@PathVariable teamName: String): TeamStats {
        return teamService.getTeamStatistics(teamName)
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
    fun getTeamAdvancedStatistics(@PathVariable teamName: String): TeamStats {
        return teamService.getTeamAdvancedStatistics(teamName)
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
        @PathVariable awayTeam: String
    ): Map<String, Double> {
        return teamService.predictMatchProbabilities(localTeam, awayTeam)
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
        @PathVariable awayTeam: String
    ): Map<String, Map<String, String>> {
        return teamService.compareTeams(localTeam, awayTeam)
    }

}