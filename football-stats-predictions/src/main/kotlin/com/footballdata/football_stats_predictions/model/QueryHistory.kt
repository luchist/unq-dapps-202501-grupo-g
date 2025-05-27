package com.footballdata.football_stats_predictions.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "query_history")
data class QueryHistory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val userName: String,

    @Column(nullable = false)
    val endpoint: String,

    @Column(nullable = false)
    val queryParameters: String,

    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val responseStatus: Int,

    @Column(length = 1000)
    val responseMessage: String? = null
)
