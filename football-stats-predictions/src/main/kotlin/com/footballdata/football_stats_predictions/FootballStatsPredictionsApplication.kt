package com.footballdata.football_stats_predictions

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

@EnableWebSecurity
@SpringBootApplication
class FootballStatsPredictionsApplication

val logger = LoggerFactory.getLogger(FootballStatsPredictionsApplication::class.java)

fun main(args: Array<String>) {
    runApplication<FootballStatsPredictionsApplication>(*args)

    logger.debug("Debug log message");
    logger.info("Info log message");
    logger.error("Error log message");
    logger.warn("Warn log message");
    logger.trace("Trace log message");
}