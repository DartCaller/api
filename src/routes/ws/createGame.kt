package com.dartcaller.routes.ws

import com.dartcaller.ActiveGamesHandlerSingleton
import com.dartcaller.dataController.GameController
import com.dartcaller.dataController.PlayerController
import com.dartcaller.dataController.ScoreController
import io.ktor.http.cio.websocket.*

class GameCreateEvent(
    val players: List<String>, val gameMode: String
) : WsEvent("GameEvent")

suspend fun createGame(event: GameCreateEvent, socket: DefaultWebSocketSession) {
    val players = event.players.map { PlayerController.create(it) }
    val scores = players.map { ScoreController.create(it, "501") }

    val newGame = GameController.create(event.gameMode, players, scores)
    socket.outgoing.send(Frame.Text(newGame.toJson()))

    ActiveGamesHandlerSingleton.add(newGame)
    ActiveGamesHandlerSingleton.subscribe(socket, newGame.id.toString())
}
