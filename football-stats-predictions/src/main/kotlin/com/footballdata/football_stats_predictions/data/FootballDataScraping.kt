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

@Component
class FootballDataScraping {

    private fun createDriver(): WebDriver {
        //System.setProperty("webdriver.chrome.driver", "C:\\tools\\chromedriver\\chromedriver.exe")
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
        visitanteTeam: String
    ): Map<String, Double> {
        val footballDataScraping = FootballDataScraping()
        val localStats = footballDataScraping.getTeamData(localTeam)
        val visitanteStats = footballDataScraping.getTeamData(visitanteTeam)

        val weights = mapOf(
            "Goles" to 0.25,
            "Tiros pp" to 0.15,
            "Posesion%" to 0.15,
            "AciertoPase%" to 0.10,
            "Aéreos" to 0.10,
            "Rating" to 0.15,
            "Yellow Cards" to 0.05,
            "Red Cards" to 0.05
        )

        fun calcScore(stats: Map<String, Double>): Double {
            var score = 0.0
            for ((k, w) in weights) {
                val v = stats[k] ?: 0.0
                // Si es porcentaje, normaliza dividiendo por 100
                val norm = if (k.contains("%") || k.contains("Posesion") || k.contains("AciertoPase")) v / 100.0 else v
                score += norm * w
            }
            return score
        }

        val scoreLocal = calcScore(localStats)
        val scoreVisitante = calcScore(visitanteStats)
        val empateFactor = 1.0 - (abs(scoreLocal - scoreVisitante) / (scoreLocal + scoreVisitante + 1e-6))
        val scoreEmpate = (scoreLocal + scoreVisitante) / 2 * empateFactor

        val expLocal = exp(scoreLocal)
        val expEmpate = exp(scoreEmpate)
        val expVisitante = exp(scoreVisitante)
        val sum = expLocal + expEmpate + expVisitante

        val probLocal = (expLocal / sum) * 100
        val probEmpate = (expEmpate / sum) * 100
        val probVisitante = (expVisitante / sum) * 100

        return mapOf(
            "Victoria Local" to String.format("%.2f", probLocal).replace(",", ".").toDouble(),
            "Empate" to String.format("%.2f", probEmpate).replace(",", ".").toDouble(),
            "Victoria Visitante" to String.format("%.2f", probVisitante).replace(",", ".").toDouble()
        )
    }

    fun getTeamAdvancedStatistics(teamName: String): Map<String, Double> {
        val stats = getTeamData(teamName)
        val advancedStats = getTeamGoalsAndShotEffectiveness(stats)
        val resultados = getTeamWinsDrawsLosses(teamName)

        return advancedStats + resultados
    }

    private fun getTeamGoalsAndShotEffectiveness(stats: Map<String, Double>): Map<String, Double> {
        val goles = stats["Goles"] ?: 0.0
        val apps = stats["Apps"] ?: 1.0 // Evita división por cero
        val tirosPP = stats["Tiros pp"] ?: 0.0

        val golesPorPartido = if (apps != 0.0) goles / apps else 0.0
        val efectividadDeTiros = if (golesPorPartido != 0.0) tirosPP / golesPorPartido else 0.0

        return mapOf(
            "Goles por Partido" to golesPorPartido,
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
            subNav.click()
    
            // Esperar a que aparezca el wrapper de fixtures
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("team-fixture-wrapper")))
    
            // Buscar todos los <a> con clase que contiene "box"
            val fixtureWrapper = driver.findElement(By.id("team-fixture-wrapper"))
            val resultBoxes = fixtureWrapper.findElements(By.cssSelector("a[class^='box ']"))
    
            // Contar cada tipo
            var wins = 0.0
            var draws = 0.0
            var losses = 0.0
            for (box in resultBoxes) {
                val clazz = box.getAttribute("class")
                when {
                    clazz.contains("box w") -> wins += 1.0
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

}