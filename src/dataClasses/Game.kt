package com.dartcaller.dataClasses

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object Games : UUIDTable() {
    val gameMode = varchar("gameMode", 50)
}

class Game(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<Game>(Games)
    var gameMode by Games.gameMode
    var players by Player via GamePlayers
    var scores by Score via GameScores

    override fun toString(): String {
        return "GameMode: ${this.gameMode}, PlayerScores: ${scores.map { "${it.player.name} ${it.score}" } }"
    }
}
