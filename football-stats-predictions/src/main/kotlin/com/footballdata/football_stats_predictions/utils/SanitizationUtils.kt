package com.footballdata.football_stats_predictions.utils

/** Utility object for sanitization.
 * This is useful for ensuring that user input or other strings do not contain unwanted characters,
 * especially when logging or displaying messages.
 */
object SanitizationUtils {

    /**
     * Sanitizes a string by removing any non-alphanumeric characters and trimming whitespace.
     * If the sanitized string is empty or null, it returns the provided default value.
     *
     * @param message The string to sanitize.
     * @param default The default value to return if the sanitized string is empty or null.
     * @return A sanitized string or the default value.
     */
    fun sanitizeString(message: String?, default: String): String {
        val sanitized = message?.replace("[^a-zA-Z0-9 ]".toRegex(), "")?.trim()
        if (sanitized.isNullOrEmpty() || sanitized.isBlank()) {
            return default
        }
        return sanitized
    }
}