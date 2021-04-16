package com.dartcaller.routes.ws

import com.dartcaller.ActiveGamesHandlerSingleton
import com.dartcaller.dataClasses.Leg
import com.dartcaller.dataClasses.ScoreEntity
import com.dartcaller.dataController.*
import io.ktor.http.cio.websocket.*
import org.jetbrains.exposed.sql.transactions.transaction

class NextLegEvent(
    val gameID: String,
) : WsEvent("GameEvent")

suspend fun nextLeg(event: NextLegEvent) {
    val game = ActiveGamesHandlerSingleton.games[event.gameID]
    game?.let { game ->
        val lastLeg = game.currentLeg
        val newLegPlayerOrder = lastLeg.playerFinishedOrder
        newLegPlayerOrder.reverse()
        val newLeg = transaction {
            val newLegEntity = LegController.create(game.gameEntity.id, lastLeg.legEntity.legIndex + 1, false, 0, 0, lastLeg.legEntity.startScore)
            newLegPlayerOrder.forEachIndexed { index, playerID ->
                LegPlayerController.create(newLegEntity.id, playerID, index)
            }
            val legPlayerScores = newLegPlayerOrder.map { it to mutableListOf<ScoreEntity>() }.toMap()
            return@transaction Leg(newLegEntity, newLegPlayerOrder.toList(), legPlayerScores)
        }
        ActiveGamesHandlerSingleton.addLeg(event.gameID, newLeg)
    }
}
