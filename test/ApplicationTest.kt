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

    @Test
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

    @Test
    fun testDartThrowAddition() {
        withTestApplication({ module(testing = true, dataSource = dataSource) }) {
            val testApplicationEngine = this
            handleWebSocketConversation("ws") { incoming, outgoing ->
                outgoing.send(
                    Frame.Text("{ \"players\": [\"Dave\", \"Bob\"], \"gameMode\": \"501\", \"type\": \"CreateGame\" }")
                )
                var lastGameState = parseIncomingWsJsonMessage<GameState>(incoming)
                lastGameState.scores.values.map {
                    assertEquals(1, it.size)
                    assertEquals("501", it[0])
                }

                lastGameState = postScores(testApplicationEngine, incoming, listOf("T20", "S0"))

                lastGameState.scores[lastGameState.currentPlayer]!!.apply {
                    assertEquals(2, size)
                    assertEquals("501", this[0])
                    assertEquals("T20S0", this[1])
                }

                lastGameState.scores[lastGameState.playerOrder[1]]!!.apply {
                    assertEquals(1, size)
                }
            }
        }
    }

    @Test
    fun scoreDoesntGoBelowZero() {
        withTestApplication({ module(testing = true, dataSource = dataSource) }) {
            val testApplicationEngine = this
            handleWebSocketConversation("ws") { incoming, outgoing ->
                outgoing.send(
                    Frame.Text("{ \"players\": [\"Dave\", \"Bob\"], \"gameMode\": \"501\", \"type\": \"CreateGame\" }")
                )
                parseIncomingWsJsonMessage<GameState>(incoming)

                val scores = MutableList(25) { "D20" }
                scores.addAll(listOf("S19", "S1"))
                val lastGameState = postScores(testApplicationEngine, incoming, scores)

                listOf(
                    Pair(lastGameState.currentPlayer, "-0-0-0"),
                    Pair(lastGameState.playerOrder[1], "-0-0-0")
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

    @Test
    fun gameEnds() {
        withTestApplication({ module(testing = true, dataSource = dataSource) }) {
            val testApplicationEngine = this
            handleWebSocketConversation("ws") { incoming, outgoing ->
                outgoing.send(
                    Frame.Text("{ \"players\": [\"Dave\", \"Bob\"], \"gameMode\": \"501\", \"type\": \"CreateGame\" }")
                )
                parseIncomingWsJsonMessage<GameState>(incoming)

                val scores = MutableList(12) { "T20" }
                scores.addAll(listOf("T20", "T19", "D12", "T20", "T20", "T7", "T20", "T19", "D12"))
                val lastGameState = postScores(testApplicationEngine, incoming, scores)

                lastGameState.currentPlayer.apply {
                    assertEquals(4, lastGameState.scores[this]!!.size)
                    assertEquals("501", lastGameState.scores[this]!![0])
                    for (i in 1..2) {
                        assertEquals("T20".repeat(3), lastGameState.scores[this]!![i])
                    }
                    assertEquals("T20T19D12", lastGameState.scores[this]!![3])
                    assertEquals(true, lastGameState.legFinished)
                }

                lastGameState.playerOrder[1].apply {
                    assertEquals(5, lastGameState.scores[this]!!.size)
                    assertEquals("501", lastGameState.scores[this]!![0])
                    for (i in 1..2) {
                        assertEquals("T20".repeat(3), lastGameState.scores[this]!![i])
                    }
                    assertEquals("-0-0-0", lastGameState.scores[this]!![3])
                    assertEquals("T20T19D12", lastGameState.scores[this]!![4])
                    assertEquals(true, lastGameState.legFinished)
                }
            }
        }
    }

    private suspend inline fun <reified T>parseIncomingWsJsonMessage(receiveChannel: ReceiveChannel<Frame>) : T {
        return jacksonObjectMapper().readValue((receiveChannel.receive() as Frame.Text).readText())
    }

    private suspend fun postScores(ctx: TestApplicationEngine, receiveChannel: ReceiveChannel<Frame>, scores: List<String>): GameState {
        if (scores.isEmpty()) throw IllegalArgumentException("ScoreList with at least one element is required")
        var lastGameState: GameState? = null
        scores.map {
            ctx.handleRequest(HttpMethod.Post, "/game/throw") { setBody(it) }
                .apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    lastGameState = parseIncomingWsJsonMessage(receiveChannel)
                }
        }
        return lastGameState!!
    }

    @AfterTest
    fun shutdown () {
        embeddedPostgres.close()
    }
}
