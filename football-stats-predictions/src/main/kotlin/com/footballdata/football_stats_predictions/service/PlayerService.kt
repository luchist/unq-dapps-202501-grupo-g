package com.footballdata.football_stats_predictions.service

import com.footballdata.football_stats_predictions.data.PlayerScraper
import com.footballdata.football_stats_predictions.model.PlayerStats
import com.footballdata.football_stats_predictions.repositories.PlayerRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PlayerService(
    @field:Autowired var playerScraper: PlayerScraper,
    @field:Autowired var playerRepository: PlayerRepository,
) {
    fun getPlayerStats(playerName: String): PlayerStats {
        return playerScraper.getPlayerData(playerName)
    }

    fun getPlayerRatingsAverage(playerName: String): Double {
        return playerScraper.getPlayerRatingsAverage(playerName)
    }

    fun comparePlayerHistory(playerName: String, year: String): Map<String, Map<String, String>> {
        return playerScraper.comparePlayerStatsWithHistory(playerName, year)
    }
}