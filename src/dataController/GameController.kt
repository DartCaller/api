package com.dartcaller.dataController

import com.dartcaller.dataClasses.GameEntity
import com.dartcaller.dataClasses.Games
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import java.util.*

object GameController {
    fun create(autoDartReco: String?, gameMode: String): GameEntity {
        val gameID = Games.insertAndGetId {
            it[this.autoDartReco] = autoDartReco
            it[this.gameMode] = gameMode
        }.value
        return GameEntity(gameID, autoDartReco, gameMode)
    }

    fun get(id: UUID): GameEntity {
        val resultRow = Games.select { Games.id eq id}.toList().last()
        return GameEntity(
            resultRow[Games.id].value,
            resultRow[Games.autoDartReco],
            resultRow[Games.gameMode]
        )
    }
}
