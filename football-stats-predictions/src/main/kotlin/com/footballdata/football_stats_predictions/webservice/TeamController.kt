package com.footballdata.football_stats_predictions.webservice

import com.footballdata.football_stats_predictions.data.FootballDataAPI
import com.footballdata.football_stats_predictions.service.TeamService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/teams")
class TeamController(@field:Autowired var teamService: TeamService) {

    @GetMapping("/{teamName}")
    fun getTeamComposition(@PathVariable teamName: String): List<FootballDataAPI.Player> {
        return teamService.getTeamComposition(teamName)
    }
}