package com.footballdata.football_stats_predictions.service

import com.footballdata.football_stats_predictions.data.FootballDataScraping
import com.footballdata.football_stats_predictions.repositories.PlayerRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PlayerService(
    @field:Autowired var footballDataScraping: FootballDataScraping,
    @field:Autowired var playerRepository: PlayerRepository,
) {
    fun getPlayerStats(playerName: String): Map<String, Double> {
        return footballDataScraping.getPlayerData(playerName)
    }

    fun getPlayerRatingsAverage(playerName: String): Double {
        return footballDataScraping.getPlayerRatingsAverage(playerName)
    }

    fun comparePlayerHistory(playerName: String, year: String): Map<String, Map<String, String>> {
        return footballDataScraping.comparePlayerStatsWithHistory(playerName, year)
    }
}