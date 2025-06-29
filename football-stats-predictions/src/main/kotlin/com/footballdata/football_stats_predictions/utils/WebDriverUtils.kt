package com.footballdata.football_stats_predictions.utils

import com.footballdata.football_stats_predictions.exceptions.ScrapingNavigationException
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

    /**
     * Clicks on a link within the sub-navigation bar of the webpage.
     * Uses a functional approach with explicit error handling.
     *
     * @param driver The WebDriver instance used to interact with the browser
     * @param linkText The exact text of the link to click in the sub-navigation bar
     * @param timeout Optional timeout duration (defaults to 30 seconds)
     * @return Result object containing either the WebDriverWait instance or an exception
     */
    fun clickOnSubNavigationLink(
        driver: WebDriver,
        linkText: String,
        timeout: Duration = Duration.ofSeconds(30)
    ): Result<WebDriverWait> {
        return runCatching {
            WebDriverWait(driver, timeout).also { wait ->
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("sub-navigation")))
                    .findElement(By.linkText(linkText))
                    .click()
            }
        }.onFailure { e ->
            when (e) {
                is org.openqa.selenium.NoSuchElementException ->
                    ScrapingNavigationException("No se encontró el enlace '$linkText' en la navegación", e)
                is org.openqa.selenium.TimeoutException ->
                    ScrapingNavigationException("Tiempo de espera agotado buscando la navegación para '$linkText'", e)
                else -> ScrapingNavigationException("Error al navegar al enlace '$linkText': ${e.message}", e)
            }.let { throw it }
        }
    }

    /**
     * Navigates to the search page with the specified term and accepts cookies.
     * Uses a functional approach combining multiple operations.
     *
     * @param driver Instantiated WebDriver
     * @param searchTerm Term to search for
     * @return Result containing Unit on success or ScrapingNavigationException on failure
     */
    fun searchAndAcceptCookies(driver: WebDriver, searchTerm: String): Result<Unit> {
        return runCatching {
            driver.get("https://es.whoscored.com/Search/?t=$searchTerm")
            val wait = WebDriverWait(driver, Duration.ofSeconds(60))

            acceptCookies(driver, wait)
                .onFailure { /* Ignore cookie acceptance errors */ }

            searchAndClickFirstResult(driver, searchTerm).getOrThrow()
        }.onFailure { e ->
            if (e !is ScrapingNavigationException) {
                throw ScrapingNavigationException("Error en la navegación para '$searchTerm': ${e.message}", e)
            } else {
                throw e
            }
        }
    }

    /**
     * Accepts cookies on the current page if the accept cookies button appears.
     * Uses a functional approach with explicit error handling.
     *
     * @param driver Instantiated WebDriver
     * @param wait Optional WebDriverWait (creates a new one with 10-second timeout if not provided)
     * @return Result object containing Unit on success or Exception on failure
     */
    private fun acceptCookies(driver: WebDriver, wait: WebDriverWait? = null): Result<Unit> {
        val cookieWait = wait ?: WebDriverWait(driver, Duration.ofSeconds(10))
        return runCatching {
            cookieWait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(".css-1wc0q5e"))
            ).click()
        }
    }

    /**
     * Navigates to the search page with the specified term and clicks on the first result.
     * Uses a functional approach with explicit error handling.
     *
     * @param driver Instantiated WebDriver
     * @param searchTerm Term to search for
     * @return Result containing Unit on success or ScrapingNavigationException on failure
     */
    private fun searchAndClickFirstResult(driver: WebDriver, searchTerm: String): Result<Unit> {
        return runCatching {
            val wait = WebDriverWait(driver, Duration.ofSeconds(60))

            wait.until(
                ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector(".search-result")),
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector(".search-error"))
                )
            )

            when {
                driver.findElements(By.cssSelector(".search-error")).isNotEmpty() ||
                        driver.findElements(By.cssSelector(".search-result")).isEmpty() -> {
                    throw ScrapingNavigationException("No se encontraron resultados para: $searchTerm")
                }
                else -> wait.until(
                    ExpectedConditions.elementToBeClickable(By.cssSelector(".search-result a"))
                ).click()
            }
        }.onFailure { e ->
            if (e !is ScrapingNavigationException) {
                throw ScrapingNavigationException("Error al buscar '$searchTerm': ${e.message}", e)
            } else {
                throw e
            }
        }
    }

    /**
     * Executes operations with a WebDriver that navigates to a search term and then clicks on a sub-navigation link.
     * Uses a functional approach combining multiple operations.
     *
     * @param searchTerm Search term
     * @param subNavLinkText Text of the sub-navigation link to click
     * @param block Lambda function containing the operations to perform
     * @return The result of type T produced by the lambda function
     * @throws ScrapingNavigationException if navigation fails
     */
    inline fun <T> withSearchAndSubNavigation(
        searchTerm: String,
        subNavLinkText: String,
        block: (WebDriver, WebDriverWait) -> T
    ): T {
        return withSearchAndAcceptCookies(searchTerm) { driver ->
            clickOnSubNavigationLink(driver, subNavLinkText)
                .fold(
                    onSuccess = { wait -> block(driver, wait) },
                    onFailure = { throw it }
                )
        }
    }

    /**
     * Executes operations with a WebDriver that navigates to a search term.
     * Uses a functional approach with resource management.
     *
     * @param searchTerm Search term
     * @param block Lambda function containing the operations to perform
     * @return The result of type T produced by the lambda function
     * @throws ScrapingNavigationException if navigation fails
     */
    inline fun <T> withSearchAndAcceptCookies(searchTerm: String, block: (WebDriver) -> T): T {
        return createDriver().use { driver ->
            searchAndAcceptCookies(driver, searchTerm)
                .fold(
                    onSuccess = { block(driver) },
                    onFailure = { throw it }
                )
        }
    }

    /**
     * Extension that allows using WebDriver with the 'use' function
     */
    inline fun <T> WebDriver.use(block: (WebDriver) -> T): T {
        try {
            return block(this)
        } finally {
            this.quit()
        }
    }
}