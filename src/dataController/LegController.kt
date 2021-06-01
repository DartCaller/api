package com.dartcaller.dataController

import com.dartcaller.dataClasses.LegEntity
import com.dartcaller.dataClasses.Legs
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
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

    fun getByGame(gameID: UUID): List<LegEntity> {
        return transaction {
            Legs.select { Legs.game eq gameID }.map {
                LegEntity(
                    it[Legs.id].value,
                    it[Legs.game].value,
                    it[Legs.legIndex],
                    it[Legs.finished],
                    it[Legs.currentPlayerTurnIndex],
                    it[Legs.currentRoundIndex],
                    it[Legs.startScore]
                )
            }
        }
    }

    fun updateCurrentPlayerTurn(id: UUID, currentPlayerTurnIndex: Int, currentRoundIndex: Int) {
        transaction {
            Legs.update ({ Legs.id eq id }) {
                it[this.currentRoundIndex] = currentRoundIndex
                it[this.currentPlayerTurnIndex] = currentPlayerTurnIndex
            }
        }
    }

    fun changeFinished(id: UUID, finished: Boolean) {
        transaction {
            Legs.update ({ Legs.id eq id }) {
                it[this.finished] = finished
            }
        }
    }
}
