package com.footballdata.football_stats_predictions

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

@EnableWebSecurity
@SpringBootApplication
class FootballStatsPredictionsApplication

fun main(args: Array<String>) {
    runApplication<FootballStatsPredictionsApplication>(*args)
}