package com.dartcaller

import com.dartcaller.routes.ws.WsEvent
import com.dartcaller.routes.ws.createGame
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.slf4j.event.Level
import java.text.DateFormat
import java.time.Duration
import javax.sql.DataSource

data class CorrectScore(val playerId: String, val scoreString: String)

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false, dataSource: DataSource? = null) {
    when {
        testing -> Database(dataSource)
        !testing -> Database()
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(ContentNegotiation) {
        gson {
            setDateFormat(DateFormat.LONG)
            setPrettyPrinting()
        }
    }

    routing {
        webSocket("/ws") {
            val mapper = jacksonObjectMapper()
            try {
            incoming.consumeAsFlow()
                .mapNotNull { it as? Frame.Text }
                .map { it.readText() }
                .map { Pair(mapper.readValue<WsEvent>(it), it) }
                .collect { (data, raw) ->
                    when (data.type) {
                        "CreateGame" -> createGame(mapper.readValue(raw), this)
                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            }
        }

        post("/game/throw") {
            ActiveGamesHandlerSingleton.addScore(call.receiveText())
            call.respond(HttpStatusCode.OK)
        }

        post("/leg/{legID}/correctScore") {
            val legID = call.parameters["legID"]
            val data = call.receive<CorrectScore>()
            if (legID != null) {
                try {
                    ActiveGamesHandlerSingleton.correctScore(legID, data)
                    call.respond(HttpStatusCode.OK)
                } catch (e: IllegalStateException) {
                    call.respond(HttpStatusCode.Conflict)
                }
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}

