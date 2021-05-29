package com.dartcaller

import com.auth0.jwk.UrlJwkProvider
import com.dartcaller.routes.ws.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
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

    install(CORS) {
        method(HttpMethod.Options)
        header(HttpHeaders.XForwardedProto)
        header(HttpHeaders.AccessControlAllowOrigin)
        allowCredentials = true
        allowNonSimpleContentTypes = true
        anyHost()
        host("localhost")
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

    install(Authentication) {
        jwt {
            realm = "Ktor auth0"
            skipWhen { testing }
            verifier(UrlJwkProvider(ConfigFactory.load().getString("auth0.issuer")))
            validate { credential ->
                val payload = credential.payload
                if (payload.audience.contains(ConfigFactory.load().getString("auth0.audience"))) {
                    JWTPrincipal(payload)
                } else {
                    null
                }
            }
        }
    }

    routing {
        webSocket("/ws") {
            val mapper = jacksonObjectMapper()
            val connection = Connection(this)
            try {
            incoming.consumeAsFlow()
                .mapNotNull { it as? Frame.Text }
                .map { it.readText() }
                .map { Pair(mapper.readValue<WsEvent>(it), it) }
                .collect { (data, raw) ->
                    when (connection.authenticated || testing) {
                        true -> when (data.type) {
                            "CreateGame" -> createGame(mapper.readValue(raw), connection)
                            "JoinGame" -> joinGame(mapper.readValue(raw), connection)
                            "NextLeg" -> nextLeg(mapper.readValue(raw))
                        }
                        false -> authenticateWs(mapper.readValue(raw), connection)
                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            }
        }

        authenticate {
            post("/board/{boardID}/throw") {
                call.parameters["boardID"]?.let {
                    try {
                        ActiveGamesHandlerSingleton.addScore(it, call.receiveText())
                        call.respond(HttpStatusCode.OK)
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }

            post("/game/{gameID}/correctScore") {
                val gameID = call.parameters["gameID"]
                val data = call.receive<CorrectScore>()
                if (gameID != null) {
                    try {
                        ActiveGamesHandlerSingleton.correctScore(gameID, data)
                        call.respond(HttpStatusCode.OK)
                    } catch (e: IllegalStateException) {
                        call.respond(HttpStatusCode.Conflict)
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }
}

