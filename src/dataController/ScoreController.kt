package com.dartcaller.dataController

import com.dartcaller.dataClasses.Player
import com.dartcaller.dataClasses.Score
import org.jetbrains.exposed.sql.transactions.transaction

object ScoreController {
    fun getAll(): List<Score> {
        return transaction {
            Score.all().toList()
        }
    }

    fun create(player: Player, score: String): Score {
        return transaction {
            Score.new { this.player = player; this.score = score }
        }
    }

    //        DartGames.update({ DartGames.gameMode eq "501" }) {
    //            with(SqlExpressionBuilder) {
    //                it[gameMode] = Concat(",", DartGames.gameMode, stringLiteral("2"))
    //            }
    //        }
}
