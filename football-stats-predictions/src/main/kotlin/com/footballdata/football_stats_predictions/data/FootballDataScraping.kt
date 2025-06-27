package com.footballdata.football_stats_predictions.data

import com.footballdata.football_stats_predictions.model.PlayerStats
import com.footballdata.football_stats_predictions.model.TeamStats
import com.footballdata.football_stats_predictions.service.StatsAnalyzer
import com.footballdata.football_stats_predictions.utils.WebDriverUtils
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class FootballDataScraping(
    @field:Autowired var statsAnalyzer: StatsAnalyzer
) {

    fun getPlayerData(playerName: String): PlayerStats {
        val driver = WebDriverUtils.createDriver()
        return try {
            WebDriverUtils.navigateAndAcceptCookies(driver, playerName)

            // Busca el body de la tabla de estadística
            val tableBody = driver.findElement(By.id("player-table-statistics-body"))
            val tableHeader = driver.findElement(By.id("player-table-statistics-head"))

            // Busca la ultima fila de la tabla de estadísticas
            val totalRow = (tableBody.findElements(By.tagName("tr"))).last()
            val namesRow = (tableHeader.findElements(By.tagName("tr"))).first()

            // Mapea los nombres de las columnas con los valores de la última fila usando totalRow y namesRow
            val cells = totalRow.findElements(By.tagName("td"))
            val namesCells = namesRow.findElements(By.tagName("th"))
            val stats = mutableMapOf<String, Double>()
            for (i in cells.indices) {
                val header = namesCells[i].text
                val value = cells[i].text
                stats[header] = value.toDoubleOrNull() ?: 0.0
            }
            PlayerStats(stats)
        } finally {
            driver.quit()
        }
    }


    fun getPlayerRatingsAverage(playerName: String): Double {
        val driver = WebDriverUtils.createDriver()
        return try {
            WebDriverUtils.navigateAndAcceptCookies(driver, playerName)
            val wait = WebDriverUtils.clickOnSubNavigationLink(driver, "Estadísticas del Partido")

            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statistics-table-summary-matches")))
            val statsDiv = driver.findElement(By.id("statistics-table-summary-matches"))
            val tableBody = statsDiv.findElement(By.id("player-table-statistics-body"))
            val ratingTds = tableBody.findElements(By.cssSelector("td.rating"))

            val ratings = ratingTds
                .mapNotNull { it.text.trim().toDoubleOrNull() }
                .take(10)

            if (ratings.isNotEmpty()) ratings.average() else 0.0
        } finally {
            driver.quit()
        }
    }

    private fun getPlayerHistoricalRatingsByYear(playerName: String, year: String): PlayerStats {
        val driver = WebDriverUtils.createDriver()
        return try {
            WebDriverUtils.navigateAndAcceptCookies(driver, playerName)
            val wait = WebDriverUtils.clickOnSubNavigationLink(driver, "Historial")

            // Esperar a que aparezca la tabla de estadísticas históricas
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statistics-table-summary")))

            val statsDiv = driver.findElement(By.id("statistics-table-summary"))

            // Obtener las cabeceras de la tabla (nombres de columnas)
            val headerRow = statsDiv.findElement(By.tagName("thead")).findElement(By.tagName("tr"))
            val headers = headerRow.findElements(By.tagName("th")).map { it.text.trim() }

            // Encontrar índices importantes
            val jgdosIndex = headers.indexOfFirst { it.contains("Jgdos") }.takeIf { it >= 0 } ?: 1
            val tppIndex = headers.indexOfFirst { it.contains("TpP") }.takeIf { it >= 0 } ?: headers.size

            // Encontrar filas del año específico
            val rows = statsDiv.findElement(By.tagName("tbody")).findElements(By.tagName("tr"))
            val targetRows = rows.filter { row ->
                val cells = row.findElements(By.tagName("td"))
                cells.isNotEmpty() && cells[0].text.trim() == year
            }

            // Para columnas con promedios, necesitamos contar contribuciones
            val combinedStats = mutableMapOf<String, Double>()
            val contributionCounts = mutableMapOf<String, Int>()

            for (row in targetRows) {
                val cells = row.findElements(By.tagName("td"))

                for (i in jgdosIndex until cells.size.coerceAtMost(headers.size)) {
                    val header = headers[i]
                    val valueText = cells[i].text.trim()
                    val value = valueText.toDoubleOrNull() ?: 0.0

                    // Solo contar contribuciones válidas
                    if (valueText.isNotEmpty() && valueText != "-") {
                        if (i >= tppIndex) {
                            contributionCounts[header] = (contributionCounts[header] ?: 0) + 1
                        }
                    }

                    combinedStats[header] = (combinedStats[header] ?: 0.0) + value
                }
            }

            // Convertir sumas a promedios para columnas desde TpP en adelante
            for (i in tppIndex until headers.size) {
                val header = headers[i]
                if (header in combinedStats && (contributionCounts[header] ?: 0) > 0) {
                    combinedStats[header] = combinedStats[header]!! / contributionCounts[header]!!
                }
            }

            PlayerStats(combinedStats)
        } finally {
            driver.quit()
        }
    }

    fun comparePlayerStatsWithHistory(playerName: String, year: String): Map<String, Map<String, String>> {
        val currentStats = getPlayerData(playerName)
        val historicalStats = getPlayerHistoricalRatingsByYear(playerName, year)
        return statsAnalyzer.compareStatsWithDiff(currentStats, historicalStats, "Actual", year)
    }


    fun getTeamData(teamName: String): TeamStats {
        val driver = WebDriverUtils.createDriver()
        return try {
            WebDriverUtils.navigateAndAcceptCookies(driver, teamName)

            // Find the team statistics table body and header
            val tableBody = driver.findElement(By.id("top-team-stats-summary-content"))
            val tableHeader = driver.findElement(By.cssSelector("#top-team-stats-summary-grid thead"))

            // Find the last row of the statistics table, and the first row of the header
            val totalRow = (tableBody.findElements(By.tagName("tr"))).last()
            val namesRow = (tableHeader.findElements(By.tagName("tr"))).first()

            // Maps the names of the columns with the values of the last row using totalRow and namesRow
            // Note: the first cell contains the "Campeonato" and "Total/Promedio" headers, so we skip it
            val cells = totalRow.findElements(By.tagName("td")).drop(1)
            val namesCells = namesRow.findElements(By.tagName("th")).drop(1)
            val stats = mutableMapOf<String, Double>()
            for (i in cells.indices) {
                val header = namesCells[i].text
                // Special handling for yellow and red cards which are in the same column, but in different colors
                if (header == "Disciplina") {
                    val yellow = cells[i].findElement(By.cssSelector(".yellow-card-box")).text
                    val red = cells[i].findElement(By.cssSelector(".red-card-box")).text
                    stats["Yellow Cards"] = yellow.toDoubleOrNull() ?: 0.0
                    stats["Red Cards"] = red.toDoubleOrNull() ?: 0.0
                } else {
                    val value = cells[i].text
                    stats[header] = value.toDoubleOrNull() ?: 0.0
                }
            }
            TeamStats(stats)
        } finally {
            driver.quit()
        }
    }

    fun getTeamAdvancedStatistics(teamName: String): TeamStats {
        val stats = getTeamData(teamName)
        val advancedStats = statsAnalyzer.getTeamGoalsAndShotEffectiveness(stats)
        val results = getTeamWinsDrawsLosses(teamName)

        return stats + advancedStats + results
    }

    private fun getTeamWinsDrawsLosses(teamName: String): TeamStats {
        val driver = WebDriverUtils.createDriver()
        return try {
            WebDriverUtils.navigateAndAcceptCookies(driver, teamName)
            val wait = WebDriverUtils.clickOnSubNavigationLink(driver, "Encuentros")
    
            // Esperar a que aparezca el wrapper de fixtures
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("team-fixture-wrapper")))
    
            // Buscar todos los <a> con clase que contiene "box"
            val fixtureWrapper = driver.findElement(By.id("team-fixture-wrapper"))
            val resultBoxes = fixtureWrapper.findElements(By.cssSelector("a[class^=' box ']"))
    
            // Contar cada tipo
            var wins = 0.0
            var draws = 0.0
            var losses = 0.0
            for (box in resultBoxes) {
                val clazz = box.getAttribute("class")
                when {
                    clazz!!.contains("box w") -> wins += 1.0
                    clazz.contains("box d") -> draws += 1.0
                    clazz.contains("box l") -> losses += 1.0
                }
            }

            TeamStats(mapOf(
                "Ganados" to wins,
                "Empatados" to draws,
                "Perdidos" to losses
            ))
        } finally {
            driver.quit()
        }
    }

    fun compareTeamStatsWithDiff(
        team1: String,
        team2: String
    ): Map<String, Map<String, String>> {
        val stats1 = getTeamData(team1)
        val stats2 = getTeamData(team2)
        return statsAnalyzer.compareStatsWithDiff(stats1, stats2, team1, team2)
    }

}