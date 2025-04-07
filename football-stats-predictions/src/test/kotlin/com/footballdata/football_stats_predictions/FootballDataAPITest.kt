package com.footballdata.football_stats_predictions

import data.FootballDataAPI
import org.junit.jupiter.api.Test

class FootballDataAPITest {
    @Test
    fun testGetTeamComposition() {
        val scrapper = FootballDataAPI()
        val listOfPlayers = scrapper.getTeamComposition("90") // Real Betis

        // Add assertions to verify the expected behavior
        assert(listOfPlayers.isNotEmpty()) { "Player list should not be empty" }
        assert(listOfPlayers[0].name.isNotEmpty()) { "Player name should not be empty" }

        println(listOfPlayers[0]);
        println(listOfPlayers[1]);
        println(listOfPlayers[2]);
        println(listOfPlayers[3]);
        println(listOfPlayers[4]);
        println(listOfPlayers[5]);
        println(listOfPlayers[6]);
        println(listOfPlayers[7]);
        println(listOfPlayers[8]);
        println(listOfPlayers[9]);
        println(listOfPlayers[10]);
        println(listOfPlayers[11]);
    }
}