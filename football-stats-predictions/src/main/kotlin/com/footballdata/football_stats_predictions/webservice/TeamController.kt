package com.footballdata.football_stats_predictions.webservice

import com.footballdata.football_stats_predictions.model.Match
import com.footballdata.football_stats_predictions.model.Player
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
@RequestMapping("/api/teams")
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
    fun getTeamComposition(
        @Parameter(
            description = "The team name that needs to be fetched",
            required = true
        )
        @PathVariable teamName: String,
        authentication: Authentication
    ): ResponseEntity<List<Player>> {
        return try {
            val players = teamService.getTeamComposition(teamName)
            authentication.let {
                queryHistoryService.saveQuery(
                    userName = it.name,
                    endpoint = "/api/teams/$teamName",
                    queryParams = "teamName=$teamName",
                    status = 200
                )
            }
            ResponseEntity.ok(players)
        } catch (e: Exception) {
            authentication.let {
                queryHistoryService.saveQuery(
                    userName = it.name,
                    endpoint = "/api/teams/$teamName",
                    queryParams = "teamName=$teamName",
                    status = 404,
                    message = e.message
                )
            }
            ResponseEntity.notFound().build()
        }
    }

    @Operation(summary = "Get scheduled matches", description = "Returns a list of scheduled Matches for a team")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "List of scheduled matches returned successfully"),
            ApiResponse(responseCode = "404", description = "Team not found or no scheduled matches")
        ]
    )
    @GetMapping("/{teamName}/matches")
    fun getScheduledMatches(
        @Parameter(
            description = "The team name for which scheduled matches are needed",
            required = true
        )
        @PathVariable teamName: String,
        authentication: Authentication
    ): ResponseEntity<List<Match>> {
        return try {
            val matches = teamService.getScheduledMatches(teamName)
            authentication.let {
                queryHistoryService.saveQuery(
                    userName = it.name,
                    endpoint = "/api/teams/$teamName/matches",
                    queryParams = "teamName=$teamName",
                    status = 200
                )
            }
            ResponseEntity.ok(matches)
        } catch (e: Exception) {
            authentication.let {
                queryHistoryService.saveQuery(
                    userName = it.name,
                    endpoint = "/api/teams/$teamName/matches",
                    queryParams = "teamName=$teamName",
                    status = 404,
                    message = e.message
                )
            }
            ResponseEntity.notFound().build()
        }
    }
}