package com.dartcaller

import com.dartcaller.dataClasses.GameState
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.server.testing.*
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import kotlinx.coroutines.channels.ReceiveChannel
import javax.sql.DataSource
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    private val embeddedPostgres: EmbeddedPostgres = EmbeddedPostgres.start()
    private val dataSource: DataSource = embeddedPostgres.postgresDatabase

//    @Test
    fun testGameCreation() {
        withTestApplication({ module(testing = true, dataSource = dataSource) }) {
            handleWebSocketConversation("ws") { incoming, outgoing ->
                outgoing.send(
                    Frame.Text("{ \"players\": [\"Dave\", \"Bob\"], \"gameMode\": \"501\", \"type\": \"CreateGame\" }")
                )
                val answer = parseIncomingWsJsonMessage<GameState>(incoming)

                assertEquals("Dave", answer.playerNames[answer.currentPlayer])
                assertEquals(answer.playerNames.entries.size, 2)
                answer.scores.values.map {
                    assertEquals(it.size, 1)
                    assertEquals(it[0], "501")
                }
                assertEquals(
                    "Dave, Bob",
                    answer.playerOrder.map { answer.playerNames[it] }.joinToString(", ")
                )
            }
        }
    }

//    @Test
    fun testDartThrowAddition() {
        withTestApplication({ module(testing = true, dataSource = dataSource) }) {
            handleWebSocketConversation("ws") { incoming, outgoing ->
                outgoing.send(
                    Frame.Text("{ \"players\": [\"Dave\", \"Bob\"], \"gameMode\": \"501\", \"type\": \"CreateGame\" }")
                )
                val initGameState = parseIncomingWsJsonMessage<GameState>(incoming)
                initGameState.scores.values.map {
                    assertEquals(1, it.size)
                    assertEquals("501", it[0])
                }

                handleRequest(HttpMethod.Post, "/game/throw") {
                    setBody("T20")
                }.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                }

                val updatedScoreGameState = parseIncomingWsJsonMessage<GameState>(incoming)
                updatedScoreGameState.scores[updatedScoreGameState.currentPlayer]!!.apply {
                    assertEquals(2, size)
                    assertEquals("501", this[0])
                    assertEquals("T20", this[1])
                }

                updatedScoreGameState.scores[updatedScoreGameState.playerOrder[1]]!!.apply {
                    assertEquals(1, size)
                }
            }
        }
    }

    @Test
    fun scoreDoesntGoBelowZero() {
        withTestApplication({ module(testing = true, dataSource = dataSource) }) {
            handleWebSocketConversation("ws") { incoming, outgoing ->
                outgoing.send(
                    Frame.Text("{ \"players\": [\"Dave\", \"Bob\"], \"gameMode\": \"501\", \"type\": \"CreateGame\" }")
                )
                var lastGameState = parseIncomingWsJsonMessage<GameState>(incoming)

                val scores = MutableList(25) { "D20" }
                scores.addAll(listOf("S19", "S1"))
                scores.map {
                    handleRequest(HttpMethod.Post, "/game/throw") {
                        setBody(it)
                    }.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        lastGameState = parseIncomingWsJsonMessage(incoming)
                    }
                }

                listOf(
                    Pair(lastGameState.currentPlayer, "-0-0-0"),
                    Pair(lastGameState.playerOrder[1], "S19-0-0")
                ).map {
                    assertEquals(6, lastGameState.scores[it.first]!!.size)
                    assertEquals("501", lastGameState.scores[it.first]!![0])
                    for (i in 1..4) {
                        assertEquals("D20".repeat(3), lastGameState.scores[it.first]!![i])
                    }
                    assertEquals(it.second, lastGameState.scores[it.first]!![5])
                }
            }
        }
    }

    private suspend inline fun <reified T>parseIncomingWsJsonMessage(receiveChannel: ReceiveChannel<Frame>) : T {
        return jacksonObjectMapper().readValue<T>((receiveChannel.receive() as Frame.Text).readText())
    }

    @AfterTest
    fun shutdown () {
        embeddedPostgres.close()
    }
}
