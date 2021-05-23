package com.dartcaller

import com.dartcaller.dataClasses.GameState
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.server.testing.*
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import javax.sql.DataSource
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationTest {
    private val embeddedPostgres: EmbeddedPostgres = EmbeddedPostgres.start()
    private val dataSource: DataSource = embeddedPostgres.postgresDatabase

    @Test
    fun testGameCreation() {
        withTestApplication({ module(testing = true, dataSource = dataSource) }) {
            handleWebSocketConversation("ws") { incoming, outgoing ->
                val answer = createGame(listOf("Dave", "Bob"), "501", incoming, outgoing)

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
    fun testDartScoreAddition() {
        withTestApplication({ module(testing = true, dataSource = dataSource) }) {
            val testApplicationEngine = this
            handleWebSocketConversation("ws") { incoming, outgoing ->
                createGame(listOf("Dave", "Bob"), "501", incoming, outgoing)

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
                createGame(listOf("Dave", "Bob"), "501", incoming, outgoing)
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
                createGame(listOf("Dave", "Bob", "Alice", ""), "501", incoming, outgoing)

                val scores = mergeLists(
                    arrayOf("D12"), Array(25) { "T20" },
                    arrayOf("S15", "T20", "T20", "T7", "T20", "T19", "D12", "T20", "T19", "D12", "D11", "D10", "T20", "T20", "S20", "T20", "T19", "D12")
                )
                val lastGameState = postScores(testApplicationEngine, incoming, scores)

                assertScores(
                    lastGameState.scores,
                    mapOf(
                        lastGameState.playerOrder[0] to listOf("501", "D12T20T20", "T20T20T20", "T20T20S15", "D11D10"),
                        lastGameState.currentPlayer to listOf("501", "T20T20T20", "T20T20T20", "-0-0-0", "-0-0-0", "T20T19D12"),
                        lastGameState.playerOrder[2] to listOf("501", "T20T20T20", "T20T20T20", "T20T19D12"),
                        lastGameState.playerOrder[3] to listOf("501", "T20T20T20", "T20T20T20", "T20T19D12")
                    )
                )
                assertEquals(true, lastGameState.legFinished)
                assertPlayerFinishOrder(lastGameState, listOf(2,3,0,1))
            }
        }
    }

    @Test
    fun correctLastScore() {
        withTestApplication({ module(testing = true, dataSource = dataSource) }) {
            val testApplicationEngine = this
            handleWebSocketConversation("ws") { incoming, outgoing ->
                createGame(listOf("Dave", "Bob"), "501", incoming, outgoing)
                var lastGameState = postScores(testApplicationEngine, incoming, mergeLists(Array(12) { "T20" }, arrayOf("T20", "T19", "D6")))

                val scoreCorrections = listOf(
                    Triple(lastGameState.playerOrder[0],"T20T19D13", HttpStatusCode.Conflict),
                    Triple(lastGameState.currentPlayer,"T20D20S20", HttpStatusCode.OK)
                )

                lastGameState = correctScores(testApplicationEngine, lastGameState, scoreCorrections, incoming)

                assertScores(
                    lastGameState.scores,
                    mapOf(
                        lastGameState.currentPlayer to listOf("501", "T20T20T20", "T20T20T20", "T20T19D6"),
                        lastGameState.playerOrder[1] to listOf("501", "T20T20T20", "T20D20S20")
                    )
                )
            }
        }
    }

    @Test
    fun correctLastScoreOnFinishedPlayer() {
        withTestApplication({ module(testing = true, dataSource = dataSource) }) {
            val testApplicationEngine = this
            handleWebSocketConversation("ws") { incoming, outgoing ->
                createGame(listOf("Dave", "Bob"), "301", incoming, outgoing)
                var lastGameState = postScores(testApplicationEngine, incoming, mergeLists(Array(6) { "T20" }, arrayOf("T17", "D25", "D10")))

                val scoreCorrections = listOf(
                    Triple(lastGameState.playerOrder[0],"T17D25S0", HttpStatusCode.OK)
                )

                lastGameState = correctScores(testApplicationEngine, lastGameState, scoreCorrections, incoming)

                assertScores(
                    lastGameState.scores,
                    mapOf(
                        lastGameState.currentPlayer to listOf("301", "T20T20T20"),
                        lastGameState.playerOrder[0] to listOf("301", "T20T20T20", "T17D25S0")
                    )
                )
                assertEquals(lastGameState.playerFinishedOrder, listOf(), "finished player array should be empty")
            }
        }
    }

    @Test
    fun correctLastScoreToFinishPlayer() {
        withTestApplication({ module(testing = true, dataSource = dataSource) }) {
            val testApplicationEngine = this
            handleWebSocketConversation("ws") { incoming, outgoing ->
                createGame(listOf("Dave", "Bob"), "301", incoming, outgoing)
                var lastGameState = postScores(testApplicationEngine, incoming, mergeLists(Array(6) { "T20" }, arrayOf("T17", "D25", "S0")))

                val scoreCorrections = listOf(
                    Triple(lastGameState.playerOrder[0],"T17D25D10", HttpStatusCode.OK)
                )

                lastGameState = correctScores(testApplicationEngine, lastGameState, scoreCorrections, incoming)

                assertScores(
                    lastGameState.scores,
                    mapOf(
                        lastGameState.currentPlayer to listOf("301", "T20T20T20"),
                        lastGameState.playerOrder[0] to listOf("301", "T20T20T20", "T17D25D10")
                    )
                )
                assertEquals(
                    lastGameState.playerFinishedOrder,
                    listOf(lastGameState.playerOrder[0]),
                    "finished players array should NOT be empty"
                )
            }
        }
    }

    private suspend fun correctScores(ctx: TestApplicationEngine, lastGameState: GameState, scoreCorrections: List<Triple<String, String, HttpStatusCode?>>, receiveChannel: ReceiveChannel<Frame>): GameState {
        with(ctx) {
            if (scoreCorrections.isEmpty()) return lastGameState
            var newGameState = lastGameState
            scoreCorrections.forEach {
                handleRequest(HttpMethod.Post, "/game/${lastGameState.gameID}/correctScore") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(buildScoreCorrectionJson(it.first, it.second))
                }.apply {
                    val responseCode = response.status()
                    val expectedCode = if (it.third != null) it.third else HttpStatusCode.OK
                    assertEquals(expectedCode, responseCode)
                    if (it.third == HttpStatusCode.OK) {
                        newGameState = parseIncomingWsJsonMessage(receiveChannel)
                    }
                }
            }
            return newGameState
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

    private fun assertPlayerFinishOrder(lastGameState: GameState, expectedFinishOrder: List<Number>) {
        expectedFinishOrder.forEachIndexed { index, number ->
            assertEquals(lastGameState.playerFinishedOrder[index], lastGameState.playerOrder[number as Int])
        }
    }

    private fun buildScoreCorrectionJson(playerId: String, scoreString: String): String {
        return "{ 'playerId': '$playerId', 'scoreString': '$scoreString' }"
            .replace("'", "\"")
    }

    private suspend fun createGame(players: List<String>, gameMode: String, incoming: ReceiveChannel<Frame>, outgoing: SendChannel<Frame>) : GameState {
        outgoing.send(buildCreateGameEvent(players, gameMode))
        return parseIncomingWsJsonMessage(incoming)
    }

    private fun buildCreateGameEvent(players: List<String>, gameMode: String) : Frame {
        return Frame.Text("{ 'players': ${players.map {"'$it'"} }, 'gameMode': '$gameMode', 'type': 'CreateGame' }".replace("'", "\""))
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
            ctx.handleRequest(HttpMethod.Post, "/board/proto/throw") { setBody(it) }
                .apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    lastGameState = parseIncomingWsJsonMessage(receiveChannel)
                }
        }
        return lastGameState!!
    }

    @AfterAll
    fun shutdown () {
        embeddedPostgres.close()
    }
}
