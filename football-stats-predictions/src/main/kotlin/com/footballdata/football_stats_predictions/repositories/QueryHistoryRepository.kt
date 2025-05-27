package com.footballdata.football_stats_predictions.repositories

import com.footballdata.football_stats_predictions.model.QueryHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface QueryHistoryRepository : JpaRepository<QueryHistory, Long> {

    //fun findByUserIdOrderByTimestampDesc(userId: Long): List<QueryHistory>

    fun findByUserNameOrderByTimestampDesc(userName: String): List<QueryHistory>

//    @Query("SELECT qh FROM QueryHistory qh WHERE qh.userId = :userId AND qh.timestamp >= :fromDate")
//    fun findByUserIdAndTimestampAfter(userId: Long, fromDate: LocalDateTime): List<QueryHistory>
}
