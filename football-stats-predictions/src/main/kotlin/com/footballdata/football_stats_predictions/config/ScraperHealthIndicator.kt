package com.footballdata.football_stats_predictions.config

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class ScraperHealthIndicator(
    private val scraperHealthChecker: ScraperHealthChecker
) : HealthIndicator {

    override fun health(): Health {
        val result = scraperHealthChecker.checkFullHealth()

        return if (result.status) {
            Health.up()
                .withDetails(result.details)
                .build()
        } else {
            Health.down()
                .withDetails(result.details + ("error" to (result.error ?: "Unknown error")))
                .build()
        }
    }
}