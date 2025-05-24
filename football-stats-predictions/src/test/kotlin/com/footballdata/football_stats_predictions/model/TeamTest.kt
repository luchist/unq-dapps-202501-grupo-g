package com.footballdata.football_stats_predictions.model

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class TeamTest {
    @Test
    fun contextLoads() {
    }

    @Test
    fun testTeamBuilder() {

        val mockPlayers = listOf(
            PlayerBuilder()
                .withPlayerName("Cristiano Ronaldo")
                .withPosition("Forward")
                .withShoots(10)
                .withInterceptions(2)
                .withDateOfBirth("05/02/1985")
                .withNationality("Portugal")
                .build(),
            PlayerBuilder()
                .withPlayerName("Lionel Messi")
                .withPosition("Forward")
                .withShoots(12)
                .withInterceptions(3)
                .withDateOfBirth("24/06/1987")
                .withNationality("Argentina")
                .build()
        ).toMutableList()

        val team = TeamBuilder()
            .withTeamName("Real Madrid")
            .withPlayers(mockPlayers)
            .build()

        assert(team.teamName == "Real Madrid")
        assert(team.players.size == 2)
    }
}