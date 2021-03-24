package com.dartcaller.dataController

import com.dartcaller.dataClasses.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object GameController {
    fun getAll(): List<Game> {
        return transaction {
            Game.all().toList()
        }
    }

    fun get(id: UUID): Game? {
        return transaction {
            Game.findById(id)
        }
    }

    fun create(gameMode: String, players: List<Player>, scores: List<Score>): Game {
        val playerOrder = players.joinToString(",") { it.id.toString() }
        return transaction {
            Game.new {
                this.gameMode = gameMode
                this.playerOrder = playerOrder
                this.currentPlayer = players[0]
                this.scores = SizedCollection(scores)
                this.players = SizedCollection(players)
            }
        }
    }

    fun addThrow(game: Game, score: String) {
        transaction {
            val currentPlayerScores = game.scores.find { it.player.id === game.currentPlayer.id }!!.score
            val currentRound = currentPlayerScores.split(",").last()
            val throwsInLastRound = currentRound.filter { listOf('S','D','T').contains(it) }.count()

            val isFirstRound = throwsInLastRound == 0
            val startNewRound = throwsInLastRound == 2
            val isNewRound = throwsInLastRound == 3
            val scoreAppendString = "${if(isNewRound || isFirstRound) ',' else ""}${score}"
            game.scores.find { it.player.id === game.currentPlayer.id }!!.score += scoreAppendString

            if (startNewRound) {
                nextTurn(game);
            }
        }
    }

    fun nextTurn(game: Game) {
        transaction {
            val playerOrderList = game.playerOrder.split(',')
            val currentPlayerIndex = playerOrderList.indexOf(game.currentPlayer.id.toString())

            val nextPlayerIndex = if (currentPlayerIndex == playerOrderList.size - 1) 0 else currentPlayerIndex + 1
            val nextPlayer = game.players.find {
                return@find it.id.toString() == playerOrderList[nextPlayerIndex]
            }
            game.currentPlayer = nextPlayer!!
        }
    }
}
