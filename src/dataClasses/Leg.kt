package com.dartcaller.dataClasses

import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object Legs : UUIDTable() {
    val game = reference("game", Games)
    val legIndex = integer("legIndex")
    var finished = bool("state")
    var currentPlayerTurnIndex = integer("currentPlayerTurnIndex")
    var currentRoundIndex = integer("currentRoundIndex")
    val startScore = integer("startScore")
}

class LegEntity(
    val id: UUID,
    val game: UUID,
    val legIndex: Int,
    var finished: Boolean,
    var currentPlayerTurnIndex: Int,
    var currentRoundIndex: Int,
    val startScore: Int
)

class Leg(
    val legEntity: LegEntity,
    val playerOrder: List<UUID>,
    val scores: Map<UUID, MutableList<ScoreEntity>>
) {
    fun getCurrentPlayerID(): UUID {
        return playerOrder[legEntity.currentPlayerTurnIndex]
    }
}
