package com.dartcaller

import com.dartcaller.dataClasses.Game
import com.dartcaller.routes.ws.Connection
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.util.concurrent.CancellationException

object ActiveGamesHandlerSingleton {
    var games = mutableMapOf<String, Game>()
    var subscribers = mutableMapOf<String, MutableList<Connection>>().withDefault { mutableListOf() }

    fun add(game: Game) {
        this.games[game.gameEntity.id.toString()] = game
    }

    fun subscribe(socket: DefaultWebSocketSession, gameId: String) {
        val subscriberList = this.subscribers.getValue(gameId)
        val connection = Connection(socket)
        subscriberList.add(connection)
        this.subscribers[gameId] = subscriberList
        println("Subscribed socket ${connection.name}")
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

    suspend fun addScore(scoreString: String) {
        games.values.forEach {
            it.addThrow(scoreString)
            updateSubscribers(it)
        }
    }

    suspend fun correctScore(legID: String, scoreToCorrect: CorrectScore) {
        for (game in games.values.iterator()) {
            if (legID == game.currentLeg.legEntity.id.toString()) {
                game.correctScore(scoreToCorrect.playerId, scoreToCorrect.scoreString)
                updateSubscribers(game)
                break
            }
        }
    }
}
