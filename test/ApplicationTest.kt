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
                parseIncomingWsJsonMessage<GameState>(incoming)

                val lastGameState = postScores(testApplicationEngine, incoming, listOf("T20", "S0"))

                assertScores(
                    lastGameState.scores,
                    mapOf(
                        lastGameState.currentPlayer to listOf("501", "T20S0"),
                        lastGameState.playerOrder[1] to listOf("501")
                    )
                )
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
                val lastGameState = postScores(testApplicationEngine, incoming, mergeLists(Array(25) { "D20" }, arrayOf("S19", "S1")))


                val expectedScore = listOf("501", "D20D20D20", "D20D20D20", "D20D20D20", "D20D20D20", "-0-0-0")
                assertScores(
                    lastGameState.scores,
                    mapOf(
                        lastGameState.currentPlayer to expectedScore,
                        lastGameState.playerOrder[1] to expectedScore
                    )
                )
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

                val scores = mergeLists((Array(12) { "T20" }), arrayOf("T20", "T19", "D12", "T20", "T20", "T7", "T20", "T19", "D12"))
                val lastGameState = postScores(testApplicationEngine, incoming, scores)

                assertScores(
                    lastGameState.scores,
                    mapOf(
                        lastGameState.currentPlayer to listOf("501", "T20T20T20", "T20T20T20", "T20T19D12"),
                        lastGameState.playerOrder[1] to listOf("501", "T20T20T20", "T20T20T20", "-0-0-0", "T20T19D12")
                    )
                )
                assertEquals(true, lastGameState.legFinished)
            }
        }
    }

    private fun assertScores(scores: Map<String, List<String>>, expectedScores: Map<String, List<String>>) {
        expectedScores.forEach {
            val playerScore = scores.getValue(it.key)
            val expectedScore = it.value
            assertEquals(expectedScore.size ,playerScore.size, "$playerScore and $expectedScore must be of same size")
            playerScore.zip(expectedScore).map { (realScore, expectedScore) ->
                assertEquals(expectedScore, realScore, "$playerScore and $expectedScore don't match for player ${it.key}")
            }
        }
    }

    private inline fun <reified T> mergeLists(vararg items: Array<T>): List<T> {
        return items.flatten()
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
