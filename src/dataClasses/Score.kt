package com.dartcaller.dataClasses

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
        } else {
            throw IllegalStateException("Cannot add score when 3 darts were already thrown")
        }
    }

    fun setScore(newScoreString: String) {
        score = convertScoreStringToScore(newScoreString)
        scoreString = newScoreString
    }
}
