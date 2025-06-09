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
            val wait = WebDriverWait(driver, Duration.ofSeconds(10))

            // Haz clic en el primer resultado del equipo
            val teamLink = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(".search-result a"))
            )
            teamLink.click()

            // Espera a que cargue la pestaña de estadísticas y haz clic
            val statsTab = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(),'Estadísticas')]"))
            )
            statsTab.click()

            // Espera a que cargue la tabla de estadísticas
            wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".stat-table"))
            )

            // Busca la fila "Total / Promedio"
            val totalRow = driver.findElement(
                By.xpath("//table[contains(@class,'stat-table')]//tr[td[contains(text(),'Total / Promedio')]]")
            )

            // Extrae los valores de las celdas de esa fila
            val cells = totalRow.findElements(By.tagName("td"))
            val stats = mutableMapOf<String, String>()
            for (cell in cells) {
                val header = cell.getAttribute("data-title") ?: continue
                val value = cell.text
                stats[header] = value
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
            } catch (_: Exception) {}

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
            val headers = headerRow.findElements(By.tagName("th")).map { it.text.trim() }.drop(1) // elimina la palabra Campeonato

            val tbody = table.findElement(By.tagName("tbody"))
            val rows = tbody.findElements(By.tagName("tr"))
            val lastRow = rows.last()
            val values = lastRow.findElements(By.tagName("td")).map { it.text.trim() }.drop(1) // elimina la palabra "Total / Promedio"

            headers.zip(values).toMap()
        } finally {
            driver.quit()
        }
    }

}