package com.dartcaller.dataController

import com.dartcaller.dataClasses.Player
import com.dartcaller.dataClasses.Players
import org.jetbrains.exposed.sql.insertAndGetId

object PlayerController {
    fun create(name: String): Player {
        val newPlayerID = Players.insertAndGetId {
            it[Players.name] = name
        }
        return Player(newPlayerID.value, name)
    }
}
