package com.dartcaller

import com.dartcaller.routes.ws.WsEvent
import com.dartcaller.routes.ws.createGame
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.slf4j.event.Level
import java.time.Duration

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    Database()

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(io.ktor.websocket.WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/ws") {
            val friendlyMapper = jacksonObjectMapper()
            friendlyMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            val strictMapper = jacksonObjectMapper()
            try {
            incoming.consumeAsFlow()
                .mapNotNull { it as? Frame.Text }
                .map { it.readText() }
                .map { Pair(friendlyMapper.readValue<WsEvent>(it), it) }
                .collect { (data, raw) ->
                    when (data.type) {
                        "CreateGame" -> createGame(strictMapper.readValue(raw))
                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            }
        }
    }
}

