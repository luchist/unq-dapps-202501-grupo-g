package com.footballdata.football_stats_predictions.model

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class PlayerTest {

    @Test
    fun contextLoads() {
    }

    @Test
    fun testPlayerBuilder() {
        val player = PlayerBuilder()
            .withPlayerName("Cristiano Ronaldo")
            .withPosition("Forward")
            .withShoots(10)
            .withInterceptions(2)
            .withDateOfBirth("05/02/1985")
            .withNationality("Portugal")
            .build()

        assert(player.playerName == "Cristiano Ronaldo")
        assert(player.position == "Forward")
        assert(player.shoots == 10)
        assert(player.interceptions == 2)
        assert(player.dateOfBirth == "05/02/1985")
        assert(player.nationality == "Portugal")
    }

}