package com.footballdata.football_stats_predictions.data

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.springframework.stereotype.Component
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration
import kotlin.ranges.until
import kotlin.text.get

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
            val wait = WebDriverWait(driver, Duration.ofSeconds(10))

            // Haz clic en el primer resultado del jugador
            val playerLink = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(".search-result a"))
            )
            playerLink.click()

            // Haz clic en la pestaña "Participaciones Actuales"
            val participacionesTab = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(),'Participaciones Actuales')]"))
            )
            participacionesTab.click()

            // Espera a que cargue la tabla de participaciones
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
}