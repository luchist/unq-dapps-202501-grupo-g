package com.footballdata.football_stats_predictions.utils

object SanitizationUtils {

    fun sanitizeString(message: String?, default: String): String {
        val sanitized = message?.replace("[^a-zA-Z0-9 ]".toRegex(), "")?.trim()
        if (sanitized.isNullOrEmpty() || sanitized.isBlank()) {
            return default
        }
        return sanitized
    }
}