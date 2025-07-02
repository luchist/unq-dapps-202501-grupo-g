package com.footballdata.football_stats_predictions.repositories

import com.footballdata.football_stats_predictions.model.Comparison
import com.footballdata.football_stats_predictions.model.ComparisonType
import org.springframework.data.jpa.repository.JpaRepository

interface ComparisonRepository : JpaRepository<Comparison, Long> {
    fun findByComparisonTypeAndEntity1NameAndEntity2Name(
        comparisonType: ComparisonType,
        entity1Name: String,
        entity2Name: String
    ): Comparison?
}