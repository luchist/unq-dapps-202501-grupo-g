package com.footballdata.football_stats_predictions.repositories

import com.footballdata.football_stats_predictions.model.Team
import org.springframework.data.jpa.repository.JpaRepository

public interface TeamRepository : JpaRepository<Team, Long> {
    fun findByTeamName(teamName: String): Team?
}