package com.footballdata.football_stats_predictions.data

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Component
import java.time.Duration
import kotlin.math.abs
import kotlin.math.exp
import kotlin.text.get
import kotlin.unaryMinus

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

    fun getPlayerData2(playerName: String): Map<String, Double> {
        val driver = createDriver()
        return try {
            navigateAndAcceptCookies(driver, playerName)

            // Encuentra la tabla y sus filas
            val table = driver.findElement(By.id("statistics-table-summary"))
            val headerRow = table.findElement(By.cssSelector("thead tr"))
            val headers =
                headerRow.findElements(By.tagName("th")).map { it.text.trim() }.drop(1) // elimina la palabra Campeonato

            val tbody = table.findElement(By.tagName("tbody"))
            val rows = tbody.findElements(By.tagName("tr"))
            val lastRow = rows.last()
            val values = lastRow.findElements(By.tagName("td")).map { it.text.trim() }.drop(1) // elimina la palabra "Total / Promedio"

            headers.zip(values).associate { (header, value) ->
                header to (value.toDoubleOrNull() ?: 0.0)
            }
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

        val weights = getWeights()
        val scoreLocal = calcScore(localStats, weights)
        val scoreVisiting = calcScore(visitingStats, weights)
        val drawFactor = 1.0 - (abs(scoreLocal - scoreVisiting) / (scoreLocal + scoreVisiting + 1e-6))
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

    private fun getWeights(): Map<String, Double> = mapOf(
        "Goles" to 0.25,
        "Tiros pp" to 0.15,
        "Posesion%" to 0.15,
        "AciertoPase%" to 0.10,
        "Aéreos" to 0.10,
        "Rating" to 0.15,
        "Yellow Cards" to 0.05,
        "Red Cards" to 0.05
    )

    private fun calcScore(stats: Map<String, Double>, weights: Map<String, Double>): Double {
        var score = 0.0
        for ((k, w) in weights) {
            val v = stats[k] ?: 0.0
            val norm = if (k.contains("%") || k.contains("Posesion") || k.contains("AciertoPase")) v / 100.0 else v
            score += norm * w
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
    
            // Hacer clic en el submenú de navegación
            val wait = WebDriverWait(driver, Duration.ofSeconds(30))
            val subNav = wait.until(ExpectedConditions.elementToBeClickable(By.id("sub-navigation")))
            val link = subNav.findElement(By.linkText("Encuentros"))
            link.click()
    
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
        val allKeys = stats1.keys + stats2.keys

        val team1Map = mutableMapOf<String, String>()
        val team2Map = mutableMapOf<String, String>()

        for (key in allKeys) {
            val v1 = stats1[key] ?: 0.0
            val v2 = stats2[key] ?: 0.0
            val diff = v1 - v2
            team1Map[key] = "$v1 (${String.format("%.2f", diff)})"
            team2Map[key] = "$v2 (${String.format("%.2f", -diff)})"
        }

        return mapOf(
            team1 to team1Map,
            team2 to team2Map
        )
    }

}