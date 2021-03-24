package com.dartcaller

import io.ktor.http.cio.websocket.*
import java.util.concurrent.atomic.AtomicInteger

data class WsEvent(val type: String, val data: Any)

class Connection(val session: DefaultWebSocketSession) {
    companion object {
        var lastId = AtomicInteger(0)
    }
    val name = "user${lastId.getAndIncrement()}"
}
