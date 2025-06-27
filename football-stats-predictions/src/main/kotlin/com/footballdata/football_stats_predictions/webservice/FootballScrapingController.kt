package com.footballdata.football_stats_predictions.webservice

import com.footballdata.football_stats_predictions.data.FootballDataScraping
import com.footballdata.football_stats_predictions.model.PlayerStats
import com.footballdata.football_stats_predictions.model.TeamStats
import com.footballdata.football_stats_predictions.service.StatsAnalyzer
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/scraping")
class FootballScrapingController(
    private val footballDataScraping: FootballDataScraping,
    private val statsAnalyzer: StatsAnalyzer
) {

    @GetMapping("/team/{teamName}")
    fun getTeamStats(@PathVariable teamName: String): TeamStats {
        return footballDataScraping.getTeamData(teamName)
    }

    @GetMapping("/player/{playerName}")
    fun getPlayerStats(@PathVariable playerName: String): PlayerStats {
        return footballDataScraping.getPlayerData(playerName)
    }

    @GetMapping("/predict/{localTeam}/{visitanteTeam}")
    fun predictMatchProbabilities(
        @PathVariable localTeam: String,
        @PathVariable visitanteTeam: String
    ): Map<String, Double> {
        return statsAnalyzer.predictMatchProbabilities(localTeam, visitanteTeam)
    }

    @GetMapping("/team/advanced/{teamName}")
    fun getTeamAdvancedStatistics(@PathVariable teamName: String): TeamStats {
        return footballDataScraping.getTeamAdvancedStatistics(teamName)
    }
}