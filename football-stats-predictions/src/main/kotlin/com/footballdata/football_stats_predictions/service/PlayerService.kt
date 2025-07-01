package com.footballdata.football_stats_predictions.service

import com.footballdata.football_stats_predictions.data.PlayerScraper
import com.footballdata.football_stats_predictions.model.*
import com.footballdata.football_stats_predictions.repositories.ComparisonRepository
import com.footballdata.football_stats_predictions.repositories.PlayerRepository
import com.footballdata.football_stats_predictions.repositories.PlayerStatsRepository
import com.footballdata.football_stats_predictions.utils.PersistenceHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PlayerService(
    @field:Autowired var playerScraper: PlayerScraper,
    @field:Autowired var playerRepository: PlayerRepository,
    @field:Autowired var playerStatsRepository: PlayerStatsRepository,
    @field:Autowired var comparisonRepository: ComparisonRepository,
    @field:Autowired private val persistenceHelper: PersistenceHelper,
) {
    fun getPlayerStats(playerName: String): PlayerStats {
        return persistenceHelper.getCachedOrFetch(
            repository = playerStatsRepository,
            findFunction = { playerStatsRepository.findByPlayerName(playerName) },
            fetchFunction = { playerScraper.getPlayerData(playerName) },
            entityMapper = { stats ->
                PlayerStatsBuilder()
                    .withPlayerName(playerName)
                    .withData((stats as PlayerStats).data)
                    .build()
            }
        )
    }

    fun getPlayerRatingsAverage(playerName: String): Double {
        return playerScraper.getPlayerRatingsAverage(playerName)
    }

    fun comparePlayerHistory(playerName: String, year: String): Map<String, Map<String, String>> {
        val comparison = persistenceHelper.getCachedOrFetch(
            repository = comparisonRepository,
            findFunction = {
                comparisonRepository.findByComparisonTypeAndEntity1NameAndEntity2Name(
                    ComparisonType.PLAYER, playerName, year
                )
            },
            fetchFunction = { playerScraper.comparePlayerStatsWithHistory(playerName, year) },
            entityMapper = { comparisonData ->
                @Suppress("UNCHECKED_CAST")
                Comparison(
                    comparisonType = ComparisonType.PLAYER,
                    entity1Name = playerName,
                    entity2Name = year,
                    comparisonData = comparisonData as Map<String, Map<String, String>>
                )
            }
        )
        return comparison.comparisonData
    }
}