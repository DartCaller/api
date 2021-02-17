package com.dartcaller.routes.ws

import com.dartcaller.dataController.GameController
import com.dartcaller.dataController.PlayerController
import com.dartcaller.dataController.ScoreController
import org.jetbrains.exposed.sql.transactions.transaction

class GameCreateEvent(
    val players: List<String>, val gameMode: String
) : WsEvent("GameEvent")

fun createGame(event: GameCreateEvent) {
    val newGame = transaction {
        val players = event.players.map { PlayerController.create(it) }

        val scores = players.map { ScoreController.create(it, "501") }

        GameController.create(event.gameMode, players, scores)
    }
}
