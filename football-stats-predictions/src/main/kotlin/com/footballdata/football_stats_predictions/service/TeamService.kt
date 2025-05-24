package com.footballdata.football_stats_predictions.service

import com.footballdata.football_stats_predictions.data.FootballDataAPI
import com.footballdata.football_stats_predictions.model.Player
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TeamService(@field:Autowired var footballDataAPI: FootballDataAPI) {

    fun getTeamComposition(teamName: String): List<Player> {
        return footballDataAPI.getTeamComposition(teamName)
    }
}