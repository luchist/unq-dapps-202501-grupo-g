package com.footballdata.football_stats_predictions.webservice

import com.footballdata.football_stats_predictions.aspects.LogFunctionCall
import com.footballdata.football_stats_predictions.aspects.Queryable
import com.footballdata.football_stats_predictions.model.PlayerStats
import com.footballdata.football_stats_predictions.service.PlayerService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/player")
@Tag(name = "Player", description = "Player endpoints for retrieving player statistics")
class PlayerController(
    private val playerService: PlayerService
) {
    @Operation(
        summary = "Player endpoint",
        description = "Returns player data for authorized users"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Player stats returned successfully"),
            ApiResponse(responseCode = "403", description = "Access denied for unauthorized users"),
            ApiResponse(responseCode = "404", description = "Player not found")
        ]
    )
    @GetMapping("/{playerName}")
    @LogFunctionCall
    @Queryable
    fun getPlayerStats(
        @PathVariable playerName: String
    ): PlayerStats {
        return playerService.getPlayerStats(playerName)
    }

    @GetMapping("/{playerName}/rating")
    @LogFunctionCall
    @Queryable
    fun getPlayerRatingsAverage(
        @PathVariable playerName: String
    ): Double {
        return playerService.getPlayerRatingsAverage(playerName)
    }

    @GetMapping("/{playerName}/compare/{year}")
    @LogFunctionCall
    @Queryable
    fun comparePlayerHistory(
        @PathVariable playerName: String,
        @PathVariable year: String
    ): Map<String, Map<String, String>> {
        return playerService.comparePlayerHistory(playerName, year)
    }
}