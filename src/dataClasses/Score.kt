package com.dartcaller.dataClasses

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object Scores : UUIDTable() {
    val player = reference("player", Players)
    val score = varchar("score", 300)
}

class Score(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<Score>(Scores)
    var player by Player referencedOn(Scores.player)
    var score by Scores.score
}
