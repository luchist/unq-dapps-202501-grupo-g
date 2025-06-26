package com.footballdata.football_stats_predictions.utils

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

object WebDriverUtils {

    fun createDriver(): WebDriver {
        return ChromeDriver()
    }

    fun navigateAndAcceptCookies(driver: WebDriver, searchTerm: String) {
        driver.get("https://es.whoscored.com/Search/?t=$searchTerm")
        val wait = WebDriverWait(driver, Duration.ofSeconds(60))

        // Aceptar cookies si aparece el botón
        try {
            val acceptCookiesButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(".css-1wc0q5e"))
            )
            acceptCookiesButton.click()
        } catch (_: Exception) {}

        // Esperar y hacer clic en el primer resultado
        val resultLink = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector(".search-result a"))
        )
        resultLink.click()
    }

    fun clickOnSubNavigationLink(driver: WebDriver, linkText: String): WebDriverWait {
        val wait = WebDriverWait(driver, Duration.ofSeconds(30))
        val subNav = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("sub-navigation")))
        val link = subNav.findElement(By.linkText(linkText))
        link.click()
        return wait
    }
}