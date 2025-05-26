package com.footballdata.football_stats_predictions.model

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

class Match(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val league: String,
    val date: String,
    val homeTeamId: Long,
    val homeTeamName: String,
    val awayTeamId: Long,
    val awayTeamName: String,
    @Transient
    var homeTeam: Team? = null,
    @Transient
    var awayTeam: Team? = null
)