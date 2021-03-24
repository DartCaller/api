package com.dartcaller

import com.dartcaller.dataClasses.Game
import com.dartcaller.dataController.GameController
import io.ktor.http.cio.websocket.*
import java.util.*

object ActiveGamesHandlerSingleton {
    var games = mutableMapOf<String, Game>()
    var subscribers = mutableMapOf<String, MutableList<DefaultWebSocketSession>>().withDefault { mutableListOf() }

    fun add(game: Game) {
        this.games[game.id.toString()] = game
    }

    fun subscribe(socket: DefaultWebSocketSession, gameId: String) {
        val subscriberList = this.subscribers.getValue(gameId)
        subscriberList.add(socket)
        this.subscribers[gameId] = subscriberList
    }

    private suspend fun updateSubscribers(gameId: UUID) {
        val newestGameState = GameController.get(gameId)

        val gameSubscribers = this.subscribers.getValue(gameId.toString())
        gameSubscribers.forEach {
            val state = newestGameState?.toJson() ?: ""
            it.outgoing.send(Frame.Text(state))
        }
    }

    suspend fun addScore(score: String) {
        games.values.forEach {
            GameController.addThrow(it, score)
            updateSubscribers(it.id.value)
        }
    }
}
