package com.dartcaller

import com.dartcaller.dataClasses.Game
import com.dartcaller.dataClasses.Leg
import com.dartcaller.routes.ws.Connection
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.util.concurrent.CancellationException

object ActiveGamesHandlerSingleton {
    var games = mutableMapOf<String, Game>()
    private var subscribers = mutableMapOf<String, MutableList<Connection>>().withDefault { mutableListOf() }

    fun add(game: Game) {
        this.games[game.gameEntity.id.toString()] = game
    }

    suspend fun subscribe(socket: Connection, gameId: String) {
        if (this.games[gameId] != null) {
            val subscriberList = this.subscribers.getValue(gameId)
            subscriberList.add(socket)
            this.subscribers[gameId] = subscriberList
            this.games[gameId]?.let {
                this.updateSubscribers(it)
            }
            println("Subscribed socket ${socket.name}")
        }
    }

    private fun unsubscribe(socket: Connection, gameId: String? = null) {
        val removeFromGames = if (gameId != null) listOf(gameId) else games.keys.toList()
        removeFromGames.map {
            val subscriberList = this.subscribers.getValue(it)
            subscriberList.removeAll { sub -> sub.name == socket.name }
        }
        println("Unsubscribed socket ${socket.name}")
    }

    private suspend fun updateSubscribers(game: Game) {
        val gameSubscribers = this.subscribers.getValue(game.gameEntity.id.toString())
        with(gameSubscribers.toMutableList()) {
            forEach {
                val state = game.toJson()
                sendToSubscriber(it, Frame.Text(state))
            }
        }
    }

    private suspend fun sendToSubscriber(socket: Connection, frame: Frame) {
        try {
            socket.session.outgoing.send(frame)
        } catch (e: ClosedReceiveChannelException) {
            unsubscribe(socket)
        } catch (e: CancellationException) {
            unsubscribe(socket)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    suspend fun addScore(boardID: String, scoreString: String) {
        ensureValidScore(scoreString)
        games.values.forEach {
            if (it.gameEntity.autoDartReco == boardID) {
                it.addThrow(scoreString)
                updateSubscribers(it)
            }
        }
    }

    suspend fun correctScore(gameID: String, scoreToCorrect: CorrectScore) {
        ensureValidScore(scoreToCorrect.scoreString)
        for (game in games.values.iterator()) {
            if (gameID == game.gameEntity.id.toString()) {
                game.correctScore(scoreToCorrect.playerId, scoreToCorrect.scoreString)
                updateSubscribers(game)
                break
            }
        }
    }

    suspend fun addLeg(gameID: String, leg: Leg) {
        games[gameID]?.let {
            it.addLeg(leg)
            updateSubscribers(it)
        }
    }

    private fun ensureValidScore(score: String) {
        if (!"^(([SDT](?:\\d{1,3}))|-0){1,3}$".toRegex().matches(score)) {
            throw IllegalArgumentException("Score arg does not follow valid form")
        }
    }
}
