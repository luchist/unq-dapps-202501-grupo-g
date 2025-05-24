package com.footballdata.football_stats_predictions.repositories

import com.footballdata.football_stats_predictions.model.Player
import org.springframework.data.jpa.repository.JpaRepository

public interface PlayerRepository : JpaRepository<Player, Long> {
    fun findByPlayerName(playerName: String): Player?
}