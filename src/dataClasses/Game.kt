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

    fun addThrow(scoreString: String) {
        val currentPlayerID = currentLeg.getCurrentPlayerID()
        val currentPlayerScores = currentLeg.scores[currentPlayerID]!!
        if (currentPlayerScores.isNotEmpty() &&
            currentPlayerScores.last().roundIndex == currentLeg.legEntity.currentRoundIndex) {
            val currentRoundScore = currentPlayerScores.last()
            currentRoundScore.addScore(scoreString)

            if (currentRoundScore.thrownDarts == 3) {
                nextTurn()
            }
        } else {
            currentPlayerScores.add(ScoreEntity(
                gameEntity.id,
                currentLeg.legEntity.id,
                currentPlayerID,
                currentLeg.legEntity.currentRoundIndex,
                scoreString
            ))
        }
    }

    private fun nextTurn() {
        if (currentLeg.legEntity.currentPlayerTurnIndex + 1 == players.values.size) {
            currentLeg.legEntity.currentPlayerTurnIndex = 0
            currentLeg.legEntity.currentRoundIndex ++
        } else {
            currentLeg.legEntity.currentPlayerTurnIndex ++
        }
    }

    fun toJson(): String {
        val serializableState = transaction {
            GameState(
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
