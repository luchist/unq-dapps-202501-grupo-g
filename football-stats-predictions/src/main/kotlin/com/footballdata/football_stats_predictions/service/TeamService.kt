package com.footballdata.football_stats_predictions.service

import com.footballdata.football_stats_predictions.data.FootballDataAPI
import com.footballdata.football_stats_predictions.data.TeamScraper
import com.footballdata.football_stats_predictions.model.*
import com.footballdata.football_stats_predictions.repositories.*
import com.footballdata.football_stats_predictions.utils.PersistenceHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TeamService(
    @field:Autowired var footballDataAPI: FootballDataAPI,
    @field:Autowired var teamScraper: TeamScraper,
    @field:Autowired var playerRepository: PlayerRepository,
    @field:Autowired var teamRepository: TeamRepository,
    @field:Autowired var teamStatsRepository: TeamStatsRepository,
    @field:Autowired var comparisonRepository: ComparisonRepository,
    @field:Autowired var matchPredictionRepository: MatchPredictionRepository,
    @field:Autowired private val persistenceHelper: PersistenceHelper,
) {

    fun getTeamComposition(teamName: String): List<Player> {
        // Check if the team exists in the database
        val cachedTeam = teamRepository.findByTeamName(teamName)

        // If the team exists, fetch the players from the database
        if (cachedTeam != null) {
            return cachedTeam.players.toList()
        }

        // If the team does not exist, fetch the players and team name from the API
        val teamComposition = footballDataAPI.getTeamComposition(teamName)
        val teamNameStr = footballDataAPI.getTeamName(teamName)

        // Save the team to the database
        val team = TeamBuilder()
            .withId(teamName.toLong())
            .withTeamName(teamNameStr)
            .withPlayers(teamComposition.toMutableList())
            .build()
        teamRepository.save(team)

        // Save each player to the database
        for (player in teamComposition) {
            val existingPlayer = playerRepository.findByPlayerName(player.playerName)
            if (existingPlayer == null) {
                playerRepository.save(player)
            }
        }
        // return the team composition
        return teamComposition
    }

    fun getScheduledMatches(teamName: String): List<Match> {
        return footballDataAPI.getScheduledMatches(teamName)
    }

    fun getTeamStatistics(teamName: String): TeamStats {
        return persistenceHelper.getCachedOrFetch(
            repository = teamStatsRepository,
            findFunction = { teamStatsRepository.findByTeamName(teamName) },
            fetchFunction = { teamScraper.getTeamData(teamName) },
            entityMapper = { stats ->
                TeamStatsBuilder()
                    .withTeamName(teamName)
                    .withData((stats as TeamStats).data)
                    .build()
            }
        )
    }

    fun getTeamAdvancedStatistics(teamName: String): TeamStats {
        val advancedTeamName = "advanced_$teamName"
        return persistenceHelper.getCachedOrFetch(
            repository = teamStatsRepository,
            findFunction = { teamStatsRepository.findByTeamName(advancedTeamName) },
            fetchFunction = { teamScraper.getTeamAdvancedStatistics(teamName) },
            entityMapper = { stats ->
                TeamStatsBuilder()
                    .withTeamName(advancedTeamName)
                    .withData((stats as TeamStats).data)
                    .build()
            }
        )
    }

    fun predictMatchProbabilities(localTeam: String, awayTeam: String): Map<String, Double> {
        val prediction = persistenceHelper.getCachedOrFetch(
            repository = matchPredictionRepository,
            findFunction = { matchPredictionRepository.findByLocalTeamAndAwayTeam(localTeam, awayTeam) },
            fetchFunction = { teamScraper.predictMatchProbabilities(localTeam, awayTeam) },
            entityMapper = { predictions ->
                @Suppress("UNCHECKED_CAST")
                MatchPrediction(
                    localTeam = localTeam,
                    awayTeam = awayTeam,
                    predictions = predictions as Map<String, Double>
                )
            }
        )
        return prediction.predictions
    }

    fun compareTeams(localTeam: String, awayTeam: String): Map<String, Map<String, String>> {
        val comparison = persistenceHelper.getCachedOrFetch(
            repository = comparisonRepository,
            findFunction = {
                comparisonRepository.findByComparisonTypeAndEntity1NameAndEntity2Name(
                    ComparisonType.TEAM, localTeam, awayTeam
                )
            },
            fetchFunction = { teamScraper.compareTeamStatsWithDiff(localTeam, awayTeam) },
            entityMapper = { comparisonData ->
                @Suppress("UNCHECKED_CAST")
                Comparison(
                    comparisonType = ComparisonType.TEAM,
                    entity1Name = localTeam,
                    entity2Name = awayTeam,
                    comparisonData = comparisonData as Map<String, Map<String, String>>
                )
            }
        )
        return comparison.comparisonData
    }
}