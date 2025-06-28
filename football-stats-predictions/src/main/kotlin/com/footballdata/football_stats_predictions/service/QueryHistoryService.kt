package com.footballdata.football_stats_predictions.service

import com.footballdata.football_stats_predictions.model.QueryHistory
import com.footballdata.football_stats_predictions.repositories.QueryHistoryRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class QueryHistoryService(@Autowired private val queryHistoryRepository: QueryHistoryRepository) {

    fun saveQuery(
        userName: String,
        endpoint: String,
        queryParams: String,
        status: Int,
        message: String? = null
    ): QueryHistory {
        val queryHistory = QueryHistory(
            userName = userName,
            endpoint = endpoint,
            queryParameters = queryParams,
            responseStatus = status,
            responseMessage = message
        )
        return queryHistoryRepository.save(queryHistory)
    }

    fun getUserQueryHistory(userName: String): List<QueryHistory> =
        queryHistoryRepository.findByUserNameOrderByTimestampDesc(userName)
}
