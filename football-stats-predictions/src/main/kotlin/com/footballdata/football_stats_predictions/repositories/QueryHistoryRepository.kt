package com.footballdata.football_stats_predictions.repositories

import com.footballdata.football_stats_predictions.model.QueryHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface QueryHistoryRepository : JpaRepository<QueryHistory, Long> {
    fun findByUserNameOrderByTimestampDesc(userName: String): List<QueryHistory>
}
