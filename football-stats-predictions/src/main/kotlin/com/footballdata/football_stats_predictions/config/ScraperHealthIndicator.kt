package com.footballdata.football_stats_predictions.config

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.URL

@Component("scraper")
class ScraperHealthIndicator : HealthIndicator {

    override fun health(): Health {
        return try {
            val url = URL("https://es.whoscored.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"
            connection.connect()

            val code = connection.responseCode
            if (code == 200) {
                Health.up().withDetail("whoscoredStatus", "Available (HTTP 200)").build()
            } else {
                Health.down().withDetail("whoscoredStatus", "Unavailable (HTTP $code)").build()
            }

        } catch (ex: Exception) {
            Health.down()
                .withDetail("whoscoredStatus", "Error: ${ex.localizedMessage}")
                .build()
        }
    }
}