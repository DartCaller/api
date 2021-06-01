package com.dartcaller.dataController

import com.dartcaller.dataClasses.ScoreEntity
import com.dartcaller.dataClasses.Scores
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

object ScoreController {
    fun create(legID: UUID, player: UUID, roundIndex: Int, score: Int, scoreString: String): ScoreEntity {
        val id = transaction {
            Scores.insertAndGetId {
                it[leg] = legID
                it[this.player] = player
                it[this.roundIndex] = roundIndex
                it[this.score] = score
                it[this.scoreString] = scoreString
            }.value
        }
        return ScoreEntity(legID, player, roundIndex, scoreString, score, id)
    }

    fun updateScore(id: UUID, score: Int, scoreString: String) {
        transaction {
            Scores.update ({ Scores.id eq id }) {
                it[this.score] = score
                it[this.scoreString] = scoreString
            }
        }
    }
}
