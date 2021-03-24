package com.dartcaller.dataClasses

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class GameState (
    val playerNames: Map<String, String>,
    val currentPlayer: String,
    val scores: Map<String, List<String>>,
    val playerOrder: List<String>
)

object Games : UUIDTable() {
    val currentPlayer = reference("currentPlayer", Players)
    val gameMode = varchar("gameMode", 50)
    val playerOrder = varchar("playerOrder", 300)
}

class Game(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<Game>(Games)
    var currentPlayer by Player referencedOn(Games.currentPlayer)
    var gameMode by Games.gameMode
    var playerOrder by Games.playerOrder
    var scores by Score via GameScores
    var players by Player via GamePlayers

    override fun toString(): String {
        return "GameMode: ${this.gameMode}, PlayerScores: ${scores.map { "${it.player.name} ${it.score}" } }"
    }

    fun toJson(): String {
        val serializableState = transaction {
            GameState(
                players.map { it.id.toString() to it.name }.toMap(),
                currentPlayer.id.toString(),
                scores.map { it.player.id.toString() to it.score.split(",") }.toMap(),
                playerOrder.split(",")
            )
        }
        return jacksonObjectMapper().writeValueAsString(serializableState)
    }
}
