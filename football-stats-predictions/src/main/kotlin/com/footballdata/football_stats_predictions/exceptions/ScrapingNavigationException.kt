package com.footballdata.football_stats_predictions.exceptions


/**
 * Exception thrown when navigation operations fail in web scraping processes.
 * This includes search failures, element navigation issues, and other web interaction errors.
 */
class ScrapingNavigationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)