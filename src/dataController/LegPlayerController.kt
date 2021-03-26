package com.dartcaller.dataController

import com.dartcaller.dataClasses.LegPlayers
import org.jetbrains.exposed.sql.insert
import java.util.*

object LegPlayerController {
    fun create(leg: UUID, player: UUID, playerTurnIndex: Int) {
        LegPlayers.insert {
            it[this.leg] = leg
            it[this.player] = player
            it[this.playerTurnIndex] = playerTurnIndex
        }
    }
}
