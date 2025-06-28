package com.footballdata.football_stats_predictions.data

import com.footballdata.football_stats_predictions.model.TeamStats
import com.footballdata.football_stats_predictions.service.StatsAnalyzer
import com.footballdata.football_stats_predictions.utils.WebDriverUtils
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TeamScraper(
    @field:Autowired var statsAnalyzer: StatsAnalyzer
) {

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