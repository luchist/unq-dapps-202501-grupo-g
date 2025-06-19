package com.footballdata.football_stats_predictions.data

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Component
import java.time.Duration
import kotlin.collections.get
import kotlin.compareTo
import kotlin.math.abs
import kotlin.math.exp
import kotlin.text.compareTo
import kotlin.text.get

@Component
class FootballDataScraping {

    private fun createDriver(): WebDriver {
        return ChromeDriver()
    }

    private fun navigateAndAcceptCookies(driver: WebDriver, searchTerm: String) {
        driver.get("https://es.whoscored.com/Search/?t=$searchTerm")
        val wait = WebDriverWait(driver, Duration.ofSeconds(60))

        // Aceptar cookies si aparece el botón
        try {
            val acceptCookiesButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(".css-1wc0q5e"))
            )
            acceptCookiesButton.click()
        } catch (_: Exception) {}

        // Esperar y hacer clic en el primer resultado (equipo o jugador)
        val resultLink = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector(".search-result a"))
        )
        resultLink.click()
    }

    fun getPlayerData(playerName: String): Map<String, Double> {
        val driver = createDriver()
        return try {
            navigateAndAcceptCookies(driver, playerName)

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
            stats
        } finally {
            driver.quit()
        }
    }

    private fun clickOnSubNavigationLink(driver: WebDriver, linkText: String): WebDriverWait {
        val wait = WebDriverWait(driver, Duration.ofSeconds(30))
        val subNav = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("sub-navigation")))
        val link = subNav.findElement(By.linkText(linkText))
        link.click()
        return wait
    }

    fun getPlayerRatingsAverage(playerName: String): Double {
        val driver = createDriver()
        return try {
            navigateAndAcceptCookies(driver, playerName)
            val wait = clickOnSubNavigationLink(driver, "Estadísticas del Partido")

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

    private fun getPlayerHistoricalRatingsByYear(playerName: String, year: String): Map<String, Double> {
        val driver = createDriver()
        return try {
            navigateAndAcceptCookies(driver, playerName)
            val wait = clickOnSubNavigationLink(driver, "Historial")

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

            combinedStats
        } finally {
            driver.quit()
        }
    }

    fun comparePlayerStatsWithHistory(playerName: String, year: String): Map<String, Map<String, String>> {
        val currentStats = getPlayerData(playerName)
        val historicalStats = getPlayerHistoricalRatingsByYear(playerName, year)
        return compareStatsWithDiff(currentStats, historicalStats, "Actual", year)
    }

    private fun compareStatsWithDiff(
        stats1: Map<String, Double>,
        stats2: Map<String, Double>,
        key1: String,
        key2: String
    ): Map<String, Map<String, String>> {
        val allKeys = stats1.keys + stats2.keys

        val map1 = mutableMapOf<String, String>()
        val map2 = mutableMapOf<String, String>()

        for (key in allKeys) {
            val v1 = stats1[key] ?: 0.0
            val v2 = stats2[key] ?: 0.0
            val diff = v1 - v2
            map1[key] = "$v1 (${String.format("%.2f", diff)})"
            map2[key] = "$v2 (${String.format("%.2f", -diff)})"
        }

        return mapOf(
            key1 to map1,
            key2 to map2
        )
    }

    fun getTeamData(teamName: String): Map<String, Double> {
        val driver = createDriver()
        return try {
            navigateAndAcceptCookies(driver, teamName)

            // Find the team statistics table body and header
            val tableBody = driver.findElement(By.id("top-team-stats-summary-content"))
            val tableHeader = driver.findElement(By.cssSelector("#top-team-stats-summary-grid thead"))

            // Find the last row of the statistics table, and first row of the header
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
            stats
        } finally {
            driver.quit()
        }
    }

    fun predictMatchProbabilities(
        localTeam: String,
        visitingTeam: String
    ): Map<String, Double> {
        val footballDataScraping = FootballDataScraping()
        val localStats = footballDataScraping.getTeamData(localTeam)
        val visitingStats = footballDataScraping.getTeamData(visitingTeam)

        val weights = getWeights(localStats)

        val scoreLocal = calcScore(localStats, weights)
        val scoreVisiting = calcScore(visitingStats, weights)

        val diff = abs(scoreLocal - scoreVisiting)
        val drawFactor = exp(-diff / 5.0) // Penaliza el empate si hay mucha diferencia
        val scoreDraw = (scoreLocal + scoreVisiting) / 2 * drawFactor

        val expLocal = exp(scoreLocal)
        val expDraw = exp(scoreDraw)
        val expVisiting = exp(scoreVisiting)
        val sum = expLocal + expDraw + expVisiting

        val probLocal = (expLocal / sum) * 100
        val probDraw = (expDraw / sum) * 100
        val probVisiting = (expVisiting / sum) * 100

        return mapOf(
            "Local Win" to String.format("%.2f", probLocal).replace(",", ".").toDouble(),
            "Draw" to String.format("%.2f", probDraw).replace(",", ".").toDouble(),
            "Visiting Win" to String.format("%.2f", probVisiting).replace(",", ".").toDouble()
        )
    }

    private fun getWeights(stats: Map<String, Double>): Map<String, Double> {
        val weights = mutableMapOf<String, Double>()
        for (key in stats.keys) {
            val value = when (key) {
                "Goles" -> 0.23
                "Tiros pp" -> 0.15
                "Posesion%" -> 0.15
                "AciertoPase%" -> 0.15
                "Aéreos" -> 0.10
                "Rating" -> 0.15
                "Yellow Cards" -> -0.02
                "Red Cards" -> -0.05
                else -> 0.0
            }
            weights[key] = value
        }
        return weights
    }

    private fun calcScore(stats: Map<String, Double>, weights: Map<String, Double>): Double {
        var score = 0.0
        for ((key, weight) in weights) {
            val value = stats[key] ?: 0.0
            score += value * weight
        }
        return score
    }

    fun getTeamAdvancedStatistics(teamName: String): Map<String, Double> {
        val stats = getTeamData(teamName)
        val advancedStats = getTeamGoalsAndShotEffectiveness(stats)
        val results = getTeamWinsDrawsLosses(teamName)

        return advancedStats + results
    }

    private fun getTeamGoalsAndShotEffectiveness(stats: Map<String, Double>): Map<String, Double> {
        val goals = stats["Goles"] ?: 0.0
        val apps = stats["Apps"] ?: 1.0 // Evita división por cero
        val tirosPP = stats["Tiros pp"] ?: 0.0

        val goalsAGame = if (apps != 0.0) goals / apps else 0.0
        val efectividadDeTiros = if (goalsAGame != 0.0) tirosPP / goalsAGame else 0.0

        return mapOf(
            "Goles por Partido" to goalsAGame,
            "Efectividad de Tiros" to efectividadDeTiros
        )
    }

    private fun getTeamWinsDrawsLosses(teamName: String): Map<String, Double> {
        val driver = createDriver()
        return try {
            navigateAndAcceptCookies(driver, teamName)
            val wait = clickOnSubNavigationLink(driver, "Encuentros")
    
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
    
            mapOf(
                "Ganados" to wins,
                "Empatados" to draws,
                "Perdidos" to losses
            )
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
        return compareStatsWithDiff(stats1, stats2, team1, team2)
    }

}