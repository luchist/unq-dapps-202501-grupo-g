package com.footballdata.football_stats_predictions.webservice

import com.footballdata.football_stats_predictions.model.Match
import com.footballdata.football_stats_predictions.model.Player
import com.footballdata.football_stats_predictions.service.TeamService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/teams")
class TeamController(@field:Autowired var teamService: TeamService) {

    @Operation(summary = "Get all team members", description = "Returns a list of Players of a Team")
    @GetMapping("/{teamName}")
    fun getTeamComposition(
        @Parameter(
            description = "The team name that needs to be fetched",
            required = true
        )
        @PathVariable teamName: String
    ): List<Player> {
        return teamService.getTeamComposition(teamName)
    }

    @Operation(summary = "Get scheduled matches", description = "Returns a list of scheduled Matches for a team")
    @GetMapping("/{teamName}/matches")
    fun getScheduledMatches(
        @Parameter(
            description = "The team name for which scheduled matches are needed",
            required = true
        )
        @PathVariable teamName: String
    ): List<Match> {
        return teamService.getScheduledMatches(teamName)
    }
}