package com.dartcaller

import com.dartcaller.dataClasses.GameState
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.cio.websocket.*
import io.ktor.server.testing.*
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import javax.sql.DataSource
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    private val embeddedPostgres: EmbeddedPostgres = EmbeddedPostgres.start()
    private val dataSource: DataSource = embeddedPostgres.postgresDatabase

    @Test
    fun testRoot() {
        withTestApplication({ module(testing = true, dataSource = dataSource) }) {
            handleWebSocketConversation("ws") { incoming, outgoing ->
                outgoing.send(
                    Frame.Text("{ \"players\": [\"Dave\", \"Bob\"], \"gameMode\": \"501\", \"type\": \"CreateGame\" }")
                )
                val mapper = jacksonObjectMapper()
                val answer = mapper.readValue<GameState>((incoming.receive() as Frame.Text).readText())

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

    @AfterTest
    fun shutdown () {
        embeddedPostgres.close()
    }
}
