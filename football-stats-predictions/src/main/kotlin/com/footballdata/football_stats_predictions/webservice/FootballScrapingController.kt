package com.footballdata.football_stats_predictions.webservice

import com.footballdata.football_stats_predictions.data.FootballDataScraping
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/scraping")
class FootballScrapingController(
    private val footballDataScraping: FootballDataScraping
) {

    @GetMapping("/team")
    fun getTeamStats(@RequestParam teamName: String): Map<String, String> {
        return footballDataScraping.getTeamData(teamName)
    }

    @GetMapping("/player")
    fun getPlayerStats(@RequestParam playerName: String): Map<String, String> {
        return footballDataScraping.getPlayerData(playerName)
    }

    @GetMapping("/player2")
    fun getPlayerStats2(@RequestParam playerName: String): Map<String, String> {
        return footballDataScraping.getPlayerData2(playerName)
    }
}