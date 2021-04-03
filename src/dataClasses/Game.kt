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
    val legID: String,
    val legFinished: Boolean,
    val playerNames: Map<String, String>,
    val currentPlayer: String,
    val scores: Map<String, List<String>>,
    val playerOrder: List<String>,
)

class Game (
    val gameEntity: GameEntity,
    playerEntities: List<Player>,
    legEntities: MutableList<Leg>
) {
    val players = playerEntities.map { it.id.toString() to it }.toMap()
    val legs: List<Leg> = legEntities.sortedBy { it.legEntity.legIndex }
    val currentLeg: Leg = legs.last()

    fun addThrow(uncheckedScoreString: String) {
        val currentPlayerID = currentLeg.getCurrentPlayerID()
        val currentPlayerScores = currentLeg.scores[currentPlayerID]!!

        val totalThrownScore = if (currentPlayerScores.size > 0) {
            currentPlayerScores.map { it.score }.reduce { acc, nextScore -> acc + nextScore }
        } else 0
        val remainingScore = currentLeg.legEntity.startScore - totalThrownScore
        val potentialNewScore = remainingScore - ScoreEntity.convertScoreStringToScore(uncheckedScoreString)
        val approvedScoreString: String = if (potentialNewScore < 0 || potentialNewScore == 1) {
            "-0"
        } else if (potentialNewScore == 0 && uncheckedScoreString[0] != 'D') {
            "-0"
        } else {
            uncheckedScoreString
        }

        if (currentPlayerScores.isNotEmpty() &&
            currentPlayerScores.last().roundIndex == currentLeg.legEntity.currentRoundIndex) {
            val currentRoundScore = currentPlayerScores.last()
            currentRoundScore.addScore(approvedScoreString)
        } else {
            currentPlayerScores.add(ScoreEntity(
                gameEntity.id,
                currentLeg.legEntity.id,
                currentPlayerID,
                currentLeg.legEntity.currentRoundIndex,
                approvedScoreString
            ))
        }

        if (currentPlayerScores.last().thrownDarts == 3) {
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

        if (currentLeg.scores[currentLeg.getCurrentPlayerID()]!!.size > 0) {
            val totalThrownScore = currentLeg.scores[currentLeg.getCurrentPlayerID()]!!
                .map { it.score }.reduce { acc, nextScore -> acc + nextScore }
            if (totalThrownScore == currentLeg.legEntity.startScore) {
                if (alreadySkippedTurns < players.values.size) {
                    nextTurn(alreadySkippedTurns+1)
                } else {
                    // All players have 0, game has ended
                    currentLeg.legEntity.finished = true
                }
            }
        }
    }

    fun toJson(): String {
        val serializableState = transaction {
            GameState(
                gameEntity.id.toString(),
                currentLeg.legEntity.id.toString(),
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
                currentLeg.playerOrder.map { it.toString() }
            )
        }
        return jacksonObjectMapper().writeValueAsString(serializableState)
    }
}
