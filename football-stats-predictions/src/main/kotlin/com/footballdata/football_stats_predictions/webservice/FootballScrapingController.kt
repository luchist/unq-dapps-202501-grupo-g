package com.footballdata.football_stats_predictions.webservice

import com.footballdata.football_stats_predictions.data.FootballDataScraping
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/scraping")
class FootballScrapingController(
    private val footballDataScraping: FootballDataScraping
) {

    @GetMapping("/team/{teamName}")
    fun getTeamStats(@PathVariable teamName: String): Map<String, Double> {
        return footballDataScraping.getTeamData(teamName)
    }

    @GetMapping("/player/{playerName}")
    fun getPlayerStats(@PathVariable playerName: String): Map<String, Double> {
        return footballDataScraping.getPlayerData(playerName)
    }

    @GetMapping("/player2/{playerName}")
    fun getPlayerStats2(@PathVariable playerName: String): Map<String, Double> {
        return footballDataScraping.getPlayerData2(playerName)
    }

    @GetMapping("/predict/{localTeam}/{visitanteTeam}")
    fun predictMatchProbabilities(
        @PathVariable localTeam: String,
        @PathVariable visitanteTeam: String
    ): Map<String, Double> {
        return footballDataScraping.predictMatchProbabilities(localTeam, visitanteTeam)
    }

    @GetMapping("/team/advanced/{teamName}")
    fun getTeamAdvancedStatistics(@PathVariable teamName: String): Map<String, Double> {
        return footballDataScraping.getTeamAdvancedStatistics(teamName)
    }
}