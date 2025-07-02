package com.footballdata.football_stats_predictions.config

import com.footballdata.football_stats_predictions.data.TeamScraper
import com.footballdata.football_stats_predictions.utils.WebDriverUtils
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class ScraperHealthChecker(
    private val teamScraper: TeamScraper
) {
    data class HealthCheckResult(
        val status: Boolean,
        val details: Map<String, Any> = emptyMap(),
        val error: String? = null
    )

    fun checkWebDriverConnection(): HealthCheckResult {
        val startTime = System.currentTimeMillis()

        return try {
            val result = WebDriverUtils.testDriverConnection()
            val elapsedTime = System.currentTimeMillis() - startTime

            result.fold(
                onSuccess = { connected ->
                    HealthCheckResult(
                        status = connected,
                        details = mapOf(
                            "responseTime" to "${elapsedTime}ms",
                            "connected" to connected
                        ),
                        error = if (!connected) "WebDriver connection failed" else null
                    )
                },
                onFailure = { ex ->
                    HealthCheckResult(
                        status = false,
                        details = mapOf("responseTime" to "${elapsedTime}ms"),
                        error = "WebDriver error: ${ex.message}"
                    )
                }
            )
        } catch (e: Exception) {
            HealthCheckResult(
                status = false,
                details = mapOf("responseTime" to "${System.currentTimeMillis() - startTime}ms"),
                error = "WebDriver unexpected error: ${e.message}"
            )
        }
    }

    fun checkScraperConnection(): HealthCheckResult {
        val startTime = System.currentTimeMillis()

        return try {
            // We tried with a popular team that should always exist
            val testTeam = "Barcelona"
            val stats = teamScraper.getTeamData(testTeam)
            val elapsedTime = System.currentTimeMillis() - startTime

            if (stats.data.isNotEmpty()) {
                HealthCheckResult(
                    status = true,
                    details = mapOf(
                        "responseTime" to "${elapsedTime}ms",
                        "dataSize" to stats.data.size
                    )
                )
            } else {
                HealthCheckResult(
                    status = false,
                    details = mapOf("responseTime" to "${elapsedTime}ms"),
                    error = "Scraper returned empty data"
                )
            }
        } catch (e: Exception) {
            HealthCheckResult(
                status = false,
                details = mapOf("responseTime" to "${System.currentTimeMillis() - startTime}ms"),
                error = "Scraper error: ${e.message}"
            )
        }
    }

    fun checkFullHealth(): HealthCheckResult {
        val timeoutInSeconds = 60L
        val startTime = System.currentTimeMillis()

        try {
            // We use a timeout to avoid undefined blocks
            val webDriverResult = withTimeout(timeoutInSeconds) { checkWebDriverConnection() }

            if (!webDriverResult.status) {
                return webDriverResult
            }

            val scraperResult = withTimeout(timeoutInSeconds) { checkScraperConnection() }
            val elapsedTime = System.currentTimeMillis() - startTime

            return if (scraperResult.status) {
                HealthCheckResult(
                    status = true,
                    details = mapOf(
                        "totalResponseTime" to "${elapsedTime}ms",
                        "webDriverCheck" to webDriverResult.details,
                        "scraperCheck" to scraperResult.details
                    )
                )
            } else {
                scraperResult.copy(
                    details = scraperResult.details + ("totalResponseTime" to "${elapsedTime}ms")
                )
            }
        } catch (e: Exception) {
            return HealthCheckResult(
                status = false,
                details = mapOf("totalResponseTime" to "${System.currentTimeMillis() - startTime}ms"),
                error = "Health check error: ${e.message}"
            )
        }
    }

    private fun <T> withTimeout(timeoutInSeconds: Long, function: () -> T): T {
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        val future = executor.submit(java.util.concurrent.Callable { function.invoke() })

        try {
            return future.get(timeoutInSeconds, TimeUnit.SECONDS)
        } catch (e: Exception) {
            future.cancel(true)
            throw e
        } finally {
            executor.shutdown()
        }
    }
}