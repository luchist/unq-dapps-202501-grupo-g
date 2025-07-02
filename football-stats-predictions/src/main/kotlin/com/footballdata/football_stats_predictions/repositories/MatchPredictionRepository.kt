package com.footballdata.football_stats_predictions.repositories

import com.footballdata.football_stats_predictions.model.MatchPrediction
import org.springframework.data.jpa.repository.JpaRepository

interface MatchPredictionRepository : JpaRepository<MatchPrediction, Long> {
    fun findByLocalTeamAndAwayTeam(localTeam: String, awayTeam: String): MatchPrediction?
}