package com.dartcaller.dataClasses

import org.jetbrains.exposed.sql.Table

object GameScores: Table() {
    val game = reference("game", Games)
    val score = reference("score", Scores)
    override val primaryKey = PrimaryKey(game, score)
}
