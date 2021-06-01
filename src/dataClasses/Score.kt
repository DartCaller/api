package com.dartcaller.dataClasses

import com.dartcaller.dataController.ScoreController
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object Scores : UUIDTable() {
    val leg = reference("leg", Legs)
    val player = reference("player", Players)
    val roundIndex = integer("roundIndex")
    val score = integer("score")
    val scoreString = varchar("scoreString", 9)
}

class ScoreEntity(
    val leg: UUID,
    val player: UUID,
    val roundIndex: Int,
    scoreString: String,
    score: Int? = null,
    id: UUID? = null,
) {
    var thrownDarts = 0
    var score = 0
    var scoreString = ""
    val id: UUID = if (id !== null) id else this.createInDatabase()

    init {
        if (score === null) {
            // Without a score we initialize this object just as if we just added our first dart
            addScore(scoreString)
        } else {
            // With everything given, we won't change anything and just set the values
            this.scoreString = scoreString
            this.score = score
            setThrownDartCount()
        }
    }

    companion object {
        fun convertScoreStringToScore(scoreString: String): Int {
            val matches = "([SDT](?:\\d{1,3}))".toRegex().findAll(scoreString)
            var resultScore = 0
            matches.toList().forEach {
                val dartRingIndicator = it.value[0]
                val dartField = it.value.substring(1).toInt()
                val newScore = when (dartRingIndicator) {
                    'T' -> dartField * 3
                    'D' -> dartField * 2
                    'S' -> dartField
                    else -> 0
                }
                resultScore += newScore
            }
            return resultScore
        }
    }

    fun addScore(newScoreString: String) {
        if (thrownDarts < 3) {
            val newScore = convertScoreStringToScore(newScoreString)
            if (newScoreString == "-0") {
                scoreString = "-0".repeat(3)
                thrownDarts = 3
                score = 0
            } else {
                scoreString += newScoreString
                thrownDarts += 1
                score += newScore
            }
            persistToDatabase()
        } else {
            throw IllegalStateException("Cannot add score when 3 darts were already thrown")
        }
    }

    fun setScore(newScoreString: String) {
        score = convertScoreStringToScore(newScoreString)
        scoreString = newScoreString
        persistToDatabase()
    }

    private fun persistToDatabase() {
        ScoreController.updateScore(this.id, this.score, this.scoreString)
    }

    private fun createInDatabase(): UUID {
        return ScoreController.create(this.leg, this.player, this.roundIndex, this.score, this.scoreString).id
    }

    private fun setThrownDartCount() {
        val matches = "([SDT](?:\\d{1,3}))".toRegex().findAll(scoreString)
        this.thrownDarts = matches.toList().size
    }
}
