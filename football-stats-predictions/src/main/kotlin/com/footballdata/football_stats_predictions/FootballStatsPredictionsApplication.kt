package com.footballdata.football_stats_predictions

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.footballdata.football_stats_predictions", "data"])
class FootballStatsPredictionsApplication

fun main(args: Array<String>) {
    runApplication<FootballStatsPredictionsApplication>(*args)
}