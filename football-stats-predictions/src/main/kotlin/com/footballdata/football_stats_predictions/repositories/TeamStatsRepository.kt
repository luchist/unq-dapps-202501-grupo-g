package com.footballdata.football_stats_predictions.repositories

import com.footballdata.football_stats_predictions.model.TeamStats
import org.springframework.data.jpa.repository.JpaRepository

interface TeamStatsRepository : JpaRepository<TeamStats, Long> {
    fun findByTeamName(teamName: String): TeamStats?
}