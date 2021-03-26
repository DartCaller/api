package com.dartcaller.dataController

import com.dartcaller.dataClasses.GameEntity
import com.dartcaller.dataClasses.Games
import org.jetbrains.exposed.sql.insertAndGetId

object GameController {
    fun create(autoDartReco: String?, gameMode: String): GameEntity {
        val gameID = Games.insertAndGetId {
            it[this.autoDartReco] = autoDartReco
            it[this.gameMode] = gameMode
        }.value
        return GameEntity(gameID, autoDartReco, gameMode)
    }
}
