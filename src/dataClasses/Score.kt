package com.dartcaller.dataClasses

import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object Scores : UUIDTable() {
    val game = reference("game", Games)
    val leg = reference("leg", Legs)
    val player = reference("player", Players)
    val roundIndex = integer("roundIndex")
    val score = integer("score")
    val scoreString = varchar("scoreString", 9)
}

class ScoreEntity(
    val game: UUID,
    val leg: UUID,
    val player: UUID,
    val roundIndex: Int,
    scoreString: String
) {
    var score = 0
    var thrownDarts = 0
    var scoreString = ""

    init {
        addScore(scoreString)
    }

    companion object {
        fun convertScoreStringToScore(scoreString: String): Int {
            val dartRingIndicator = scoreString[0]
            val dartField = scoreString.substring(1).toInt()
            return when (dartRingIndicator) {
                'T' -> dartField * 3
                'D' -> dartField * 2
                'S' -> dartField
                else -> 0
            }
        }
    }

    fun addScore(newScoreString: String) {
        if (thrownDarts < 3) {
            val newScore = convertScoreStringToScore(newScoreString)
            score += newScore
            if (newScoreString == "-0") {
                scoreString = "-0".repeat(3)
                thrownDarts = 3
                score = 0
            } else {
                scoreString += newScoreString
                thrownDarts += 1
            }
        } else {
            throw IllegalStateException("Cannot add score when 3 darts were already thrown")
        }
    }
}
