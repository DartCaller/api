package com.dartcaller.routes.ws

import com.dartcaller.ActiveGamesHandlerSingleton

class GameJoinEvent(
    val gameID: String,
) : WsEvent("GameEvent")

suspend fun joinGame(event: GameJoinEvent, socket: Connection) {
    ActiveGamesHandlerSingleton.subscribe(socket, event.gameID)
}
