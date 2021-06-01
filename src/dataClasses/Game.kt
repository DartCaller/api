package com.dartcaller.dataClasses

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object Games : UUIDTable() {
    val gameMode = varchar("gameMode", 50)
    val autoDartReco = varchar("autoVarChar", 50).nullable()
}

class GameEntity (
    var id: UUID,
    val autoDartReco: String?,
    val gameMode: String
)

class GameState (
    val gameID: String,
    val legFinished: Boolean,
    val playerNames: Map<String, String>,
    val currentPlayer: String,
    val scores: Map<String, List<String>>,
    val playerOrder: List<String>,
    val playerFinishedOrder: List<String>,
    val currentRoundIndex: Int
)

class Game (
    val gameEntity: GameEntity,
    playerEntities: List<Player>,
    legEntities: MutableList<Leg>
) {
    val players = playerEntities.map { it.id.toString() to it }.toMap()
    var legs: List<Leg> = legEntities.sortedBy { it.legEntity.legIndex }
    val currentLeg: Leg
        get() = legs.last()

    fun addLeg(leg: Leg) {
        legs = legs.plus(leg).sortedBy { it.legEntity.legIndex }
    }

    fun addThrow(uncheckedScoreString: String) {
        val currentPlayerID = currentLeg.getCurrentPlayerID()
        val currentPlayerScores = currentLeg.scores[currentPlayerID]!!

        val remainingScore = remainingScore(currentLeg.getCurrentPlayerID())
        val approvedScoreString: String = if (isNewScoreAllowed(remainingScore, uncheckedScoreString)) uncheckedScoreString else "-0"

        if (currentPlayerScores.isNotEmpty() && currentPlayerScores.last().roundIndex == currentLeg.legEntity.currentRoundIndex) {
            val currentRoundScore = currentPlayerScores.last()
            currentRoundScore.addScore(approvedScoreString)
        } else {
            currentPlayerScores.add(ScoreEntity(
                currentLeg.legEntity.id,
                currentPlayerID,
                currentLeg.legEntity.currentRoundIndex,
                approvedScoreString
            ))
        }

        if (remainingScore(currentPlayerID) == 0) {
            currentLeg.playerFinishedOrder.add(currentPlayerID)
            nextTurn()
        } else if (currentPlayerScores.last().thrownDarts == 3) {
            nextTurn()
        }
    }

    private fun nextTurn(alreadySkippedTurns: Int = 0) {
        if (currentLeg.legEntity.currentPlayerTurnIndex + 1 == players.values.size) {
            currentLeg.legEntity.currentPlayerTurnIndex = 0
            currentLeg.legEntity.currentRoundIndex ++
        } else {
            currentLeg.legEntity.currentPlayerTurnIndex ++
        }

        if (0 == remainingScore(currentLeg.getCurrentPlayerID())) {
            if (alreadySkippedTurns < players.values.size) {
                nextTurn(alreadySkippedTurns+1)
            } else {
                // All players have 0, game has ended
                currentLeg.legEntity.finished = true
            }
        }
    }

    fun correctScore(playerId: String, scoreString: String) {
        val playerUUID = UUID.fromString(playerId)
        currentLeg.scores[playerUUID]?.let { playerScores ->
            playerScores.lastOrNull()?.let {
                val remainingScore = remainingScore(playerUUID, (playerScores.size - 1))
                if (isNewScoreAllowed(remainingScore, scoreString)) {
                    val oldRemainingScore = remainingScore(playerUUID)
                    it.setScore(scoreString)
                    val newRemainingScore = remainingScore(playerUUID)
                    if (oldRemainingScore == 0 && newRemainingScore > 0) {
                        currentLeg.playerFinishedOrder.remove(playerUUID)
                    }
                    if (oldRemainingScore > 0 && newRemainingScore == 0) {
                        currentLeg.playerFinishedOrder.add(playerUUID)
                    }
                } else {
                    throw IllegalStateException("Can't update total score to < 0")
                }
            }
        }
    }

    private fun remainingScore(playerId: UUID, roundIndex: Int? = null): Int {
        var playerScore = currentLeg.scores[playerId]
        if (roundIndex !== null) {
            playerScore = playerScore!!.subList(0, roundIndex)
        }
        val totalThrownScore = if (playerScore!!.size > 0) {
            playerScore
                .map { it.score }
                .reduce { acc, nextScore -> acc + nextScore }
        } else 0
        return currentLeg.legEntity.startScore - totalThrownScore
    }

    private fun isNewScoreAllowed(remainingScore: Int, uncheckedScoreString: String): Boolean {
        val matches = "([SDT](?:\\d{1,3}))".toRegex().findAll(uncheckedScoreString)
        val lastDart = matches.toList().last().value
        val potentialNewScore = remainingScore - ScoreEntity.convertScoreStringToScore(uncheckedScoreString)
        return when {
            potentialNewScore < 0 -> false
            potentialNewScore == 1 -> false
            (potentialNewScore == 0 && lastDart[0] != 'D') -> false
            else -> true
        }
    }

    fun toJson(prettyPrint: Boolean = false): String {
        val serializableState = transaction {
            GameState(
                gameEntity.id.toString(),
                currentLeg.legEntity.finished,
                players.entries.associate { it.key to it.value.name },
                currentLeg.getCurrentPlayerID().toString(),
                currentLeg.scores.entries.associate {
                    it.key.toString() to (
                        listOf(currentLeg.legEntity.startScore.toString())
                        +
                        it.value.map { score -> score.scoreString }
                    )
                },
                currentLeg.playerOrder.map { it.toString() },
                currentLeg.playerFinishedOrder.map { it.toString() },
                currentLeg.legEntity.currentRoundIndex
            )
        }
        val mapper = jacksonObjectMapper()
        if (prettyPrint) return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(serializableState)
        return mapper.writeValueAsString(serializableState)
    }
}
