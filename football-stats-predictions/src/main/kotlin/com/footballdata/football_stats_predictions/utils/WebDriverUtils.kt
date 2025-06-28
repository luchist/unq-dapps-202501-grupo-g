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

    /**
     * Extensión que permite usar WebDriver con la función 'use'
     */
    inline fun <T> WebDriver.use(block: (WebDriver) -> T): T {
        try {
            return block(this)
        } finally {
            this.quit()
        }
    }

    /**
     * Ejecuta operaciones con un WebDriver que se cierra automáticamente al finalizar.
     * Implementa el patrón resource-try-with-resources usando la función 'use' de Kotlin.
     * Navega a la URL especificada y acepta las cookies automáticamente.
     *
     * @param url URL o término de búsqueda para navegar
     * @param block Función lambda que contiene las operaciones a realizar con el WebDriver
     * @return El resultado de tipo T producido por la función lambda
     */
    inline fun <T> withDriver(url: String, block: (WebDriver) -> T): T {
        val driver = createDriver()
        return driver.use {
            navigateAndAcceptCookies(it, url)
            block(it)
        }
    }
}