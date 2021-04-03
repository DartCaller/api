package com.dartcaller.dataController

import com.dartcaller.dataClasses.LegEntity
import com.dartcaller.dataClasses.Legs
import org.jetbrains.exposed.sql.insertAndGetId
import java.util.*

object LegController {
    fun create(gameID: UUID, legIndex: Int, finished: Boolean, currentPlayerTurnIndex: Int, currentRoundIndex: Int, startScore: Int): LegEntity {
        val legID = Legs.insertAndGetId {
            it[this.game] = gameID
            it[this.legIndex] = legIndex
            it[this.finished] = finished
            it[this.currentPlayerTurnIndex] = currentPlayerTurnIndex
            it[this.currentRoundIndex] = currentRoundIndex
            it[this.startScore] = startScore
        }.value
        return LegEntity(
            legID,
            gameID,
            legIndex,
            finished,
            currentPlayerTurnIndex,
            currentRoundIndex,
            startScore
        )
    }
}
