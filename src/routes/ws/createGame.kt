package com.dartcaller.routes.ws

import com.dartcaller.ActiveGamesHandlerSingleton
import com.dartcaller.dataClasses.*
import com.dartcaller.dataController.*
import io.ktor.http.cio.websocket.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class GameCreateEvent(
    val players: List<String>, val gameMode: String
) : WsEvent("GameEvent")

fun sanitizePlayerName(string: String): String {
    return "[^a-zA-Z]".toRegex().replace(string, "")
}

suspend fun createGame(event: GameCreateEvent, socket: Connection) {
    val game = transaction {
        val startScore = if (event.gameMode == "301") 301 else 501

        val playerEntities = event.players.map { PlayerController.create(sanitizePlayerName(it)) }
        val newGameEntity = GameController.create("proto", event.gameMode)
        val newLegEntity = LegController.create(newGameEntity.id, 0, false, 0, 0, startScore)
        playerEntities.forEachIndexed { index, playerEntity ->
            GamePlayerController.create(newGameEntity.id, playerEntity.id)
            LegPlayerController.create(newLegEntity.id, playerEntity.id, index)
        }
        val legPlayerOrder = playerEntities.map { it.id }
        val legPlayerScores = playerEntities.map { it.id to mutableListOf<ScoreEntity>() }.toMap()
        val newLeg = Leg(newLegEntity, legPlayerOrder, legPlayerScores)
        return@transaction Game(newGameEntity, playerEntities, mutableListOf(newLeg))
    }
    ActiveGamesHandlerSingleton.add(game)
    ActiveGamesHandlerSingleton.subscribe(socket, game.gameEntity.id.toString())
}
