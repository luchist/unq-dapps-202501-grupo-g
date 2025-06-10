package com.footballdata.football_stats_predictions.data

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class FootballDataScraping {

    private fun createDriver(): WebDriver {
        //System.setProperty("webdriver.chrome.driver", "C:\\tools\\chromedriver\\chromedriver.exe")
        return ChromeDriver()
    }

    fun getTeamData(teamName: String): Map<String, String> {
        val driver = createDriver()
        return try {
            driver.get("https://es.whoscored.com/Search/?t=$teamName")
            val wait = WebDriverWait(driver, Duration.ofSeconds(60))

            // Accept cookie banner if it appears
            try {
                val acceptCookiesButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.cssSelector(".css-1wc0q5e"))
                )
                acceptCookiesButton.click()
            } catch (e: Exception) {
                // Continue if the button does not appear
            }

            // Wait for the search results to appear
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".search-result"))
            )

            // Wait for the team results to appear
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.xpath("//h2[contains(text(),'Equipos:')]"))
            )

            // Find the first team link in the search results and click it

            val teamLink = wait.until(
                ExpectedConditions.elementToBeClickable(
                    By.xpath("//h2[contains(text(),'Equipos:')]/following-sibling::table//tr[position()>1][1]//a")
                )
            )
            teamLink.click()

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
            val stats = mutableMapOf<String, String>()
            for (i in cells.indices) {
                val header = namesCells[i].text
                // Special handling for yellow and red cards which are in the same column, but in different colors
                if (header == "Disciplina") {
                    stats["Yellow Cards"] = cells[i].findElement(By.cssSelector(".yellow-card-box")).text
                    stats["Red Cards"] = cells[i].findElement(By.cssSelector(".red-card-box")).text
                } else {
                    val value = cells[i].text
                    stats[header] = value
                }
            }

            stats
        } finally {
            driver.quit()
        }
    }

    fun getPlayerData(playerName: String): Map<String, String> {
        val driver = createDriver()
        return try {
            driver.get("https://es.whoscored.com/Search/?t=$playerName")
            val wait = WebDriverWait(driver, Duration.ofSeconds(60))

            // Haz clic en Aceptar cookies si es necesario
            try {
                val acceptCookiesButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.cssSelector(".css-1wc0q5e"))
                )
                acceptCookiesButton.click()
            } catch (e: Exception) {
                // Si no aparece el botón, continuamos
            }

            // Haz clic en el primer resultado del jugador
            val playerLink = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(".search-result a"))
            )
            playerLink.click()

            // Espera a que cargue la tabla de participaciones
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("statistics-table-summary"))
            )

            // Busca el body de la tabla de estadística
            val tableBody = driver.findElement(By.id("player-table-statistics-body"))
            val tableHeader = driver.findElement(By.id("player-table-statistics-head"))

            // Busca la ultima fila de la tabla de estadísticas
            val totalRow = (tableBody.findElements(By.tagName("tr"))).last()
            val namesRow = (tableHeader.findElements(By.tagName("tr"))).first()

            // Mapea los nombre de las columnas con los valores de la última fila usando totalRow y namesRow
            val cells = totalRow.findElements(By.tagName("td"))
            val namesCells = namesRow.findElements(By.tagName("th"))
            val stats = mutableMapOf<String, String>()
            for (i in cells.indices) {
                val header = namesCells[i].text
                val value = cells[i].text
                stats[header] = value
            }

            stats
        } finally {
            driver.quit()
        }
    }

    fun getPlayerData2(playerName: String): Map<String, String> {
        val driver = createDriver()
        return try {
            driver.get("https://es.whoscored.com/Search/?t=$playerName")
            val wait = WebDriverWait(driver, Duration.ofSeconds(60))

            // Aceptar cookies si aparece el botón
            try {
                val acceptCookiesButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.cssSelector(".css-1wc0q5e"))
                )
                acceptCookiesButton.click()
            } catch (_: Exception) {
            }

            // Haz clic en el primer resultado del jugador
            val playerLink = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(".search-result a"))
            )
            playerLink.click()

            // Espera a que cargue la tabla de estadísticas
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("statistics-table-summary"))
            )

            // Encuentra la tabla y sus filas
            val table = driver.findElement(By.id("statistics-table-summary"))
            val headerRow = table.findElement(By.cssSelector("thead tr"))
            val headers =
                headerRow.findElements(By.tagName("th")).map { it.text.trim() }.drop(1) // elimina la palabra Campeonato

            val tbody = table.findElement(By.tagName("tbody"))
            val rows = tbody.findElements(By.tagName("tr"))
            val lastRow = rows.last()
            val values = lastRow.findElements(By.tagName("td")).map { it.text.trim() }
                .drop(1) // elimina la palabra "Total / Promedio"

            headers.zip(values).toMap()
        } finally {
            driver.quit()
        }
    }

}