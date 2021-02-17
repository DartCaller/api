package com.dartcaller.dataController

import com.dartcaller.dataClasses.Game
import com.dartcaller.dataClasses.Player
import com.dartcaller.dataClasses.Score
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object GameController {
    fun getAll(): List<Game> {
        return transaction {
            Game.all().toList()
        }
    }

    fun create(gameMode: String, players: List<Player>, scores: List<Score>): Game {
        return transaction {
            Game.new (UUID.randomUUID()) {
                this.gameMode = gameMode
                this.players = SizedCollection(players)
                this.scores = SizedCollection(scores)
            }
        }
    }
}
