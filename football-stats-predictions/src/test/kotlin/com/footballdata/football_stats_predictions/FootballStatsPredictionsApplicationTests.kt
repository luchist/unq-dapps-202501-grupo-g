package com.footballdata.football_stats_predictions

import com.footballdata.football_stats_predictions.model.PlayerBuilder
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class FootballStatsPredictionsApplicationTests {

    @Test
    fun contextLoads() {
    }

    @Test
    fun testPlayerBuilder() {
        val player = PlayerBuilder()
            .withName("Cristiano Ronaldo")
            .withPosition("Forward")
            .withShoots(10)
            .withInterceptions(2)
            .build()

        assert(player.playerName == "Cristiano Ronaldo")
        assert(player.position == "Forward")
        assert(player.shoots == 10)
        assert(player.interceptions == 2)
    }

}
