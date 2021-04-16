package com.dartcaller.routes.ws

import com.dartcaller.ActiveGamesHandlerSingleton
import io.ktor.http.cio.websocket.*

class GameJoinEvent(
    val gameID: String,
) : WsEvent("GameEvent")

suspend fun joinGame(event: GameJoinEvent, socket: DefaultWebSocketSession) {
    ActiveGamesHandlerSingleton.subscribe(socket, event.gameID)
}
