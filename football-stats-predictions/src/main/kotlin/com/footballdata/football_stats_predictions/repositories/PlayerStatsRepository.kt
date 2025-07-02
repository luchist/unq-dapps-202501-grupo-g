package com.footballdata.football_stats_predictions.repositories

import com.footballdata.football_stats_predictions.model.PlayerStats
import org.springframework.data.jpa.repository.JpaRepository

interface PlayerStatsRepository : JpaRepository<PlayerStats, Long> {
    fun findByPlayerName(playerName: String): PlayerStats?
}