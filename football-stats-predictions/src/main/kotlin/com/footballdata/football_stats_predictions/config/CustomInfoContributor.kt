package com.footballdata.football_stats_predictions.config

import com.footballdata.football_stats_predictions.repositories.PlayerStatsRepository
import com.footballdata.football_stats_predictions.repositories.TeamStatsRepository
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class CustomInfoContributor(
    private val teamStatsRepository: TeamStatsRepository,
    private val playerStatsRepository: PlayerStatsRepository
) : InfoContributor {

    override fun contribute(builder: Info.Builder) {
        val appInfo = mutableMapOf<String, Any>()

        // Información básica de la aplicación
        appInfo["name"] = "Football Stats Predictions"
        appInfo["version"] = "1.0.0"

        // Estadísticas de caché
        val cacheStats = mutableMapOf<String, Any>()
        cacheStats["teamsStats"] = teamStatsRepository.count()
        cacheStats["playersStats"] = playerStatsRepository.count()
        cacheStats["lastUpdated"] = LocalDateTime.now().toString()

        appInfo["cache"] = cacheStats

        // Información del sistema
        val systemInfo = mutableMapOf<String, Any>()
        systemInfo["javaVersion"] = System.getProperty("java.version")
        systemInfo["availableProcessors"] = Runtime.getRuntime().availableProcessors()
        systemInfo["freeMemory"] = Runtime.getRuntime().freeMemory()

        appInfo["system"] = systemInfo

        builder.withDetails(appInfo)
    }
}