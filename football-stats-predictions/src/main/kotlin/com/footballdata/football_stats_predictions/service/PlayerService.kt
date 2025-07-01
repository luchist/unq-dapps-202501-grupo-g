package com.footballdata.football_stats_predictions.service

import com.footballdata.football_stats_predictions.data.PlayerScraper
import com.footballdata.football_stats_predictions.model.PlayerStats
import com.footballdata.football_stats_predictions.model.PlayerStatsBuilder
import com.footballdata.football_stats_predictions.repositories.PlayerRepository
import com.footballdata.football_stats_predictions.repositories.PlayerStatsRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PlayerService(
    @field:Autowired var playerScraper: PlayerScraper,
    @field:Autowired var playerRepository: PlayerRepository,
    @field:Autowired var playerStatsRepository: PlayerStatsRepository
) {
    fun getPlayerStats(playerName: String): PlayerStats {
        // Buscar primero en la base de datos
        val cachedStats = playerStatsRepository.findByPlayerName(playerName)

        if (cachedStats != null) {
            return cachedStats
        }

        // Si no existe, obtener desde la fuente externa
        val stats = playerScraper.getPlayerData(playerName)

        // Crear y guardar la entidad PlayerStats
        val playerStats = PlayerStatsBuilder()
            .withPlayerName(playerName)
            .withData(stats.data)
            .build()

        return playerStatsRepository.save(playerStats)
    }

    fun getPlayerRatingsAverage(playerName: String): Double {
        return playerScraper.getPlayerRatingsAverage(playerName)
    }

    fun comparePlayerHistory(playerName: String, year: String): Map<String, Map<String, String>> {
        return playerScraper.comparePlayerStatsWithHistory(playerName, year)
    }
}