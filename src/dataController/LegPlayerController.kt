package com.dartcaller.dataController

import com.dartcaller.dataClasses.LegPlayers
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.util.*

object LegPlayerController {
    fun create(leg: UUID, player: UUID, playerTurnIndex: Int) {
        LegPlayers.insert {
            it[this.leg] = leg
            it[this.player] = player
            it[this.playerTurnIndex] = playerTurnIndex
        }
    }

    fun getByLeg(legID: UUID): Map<UUID, Int> {
        return LegPlayers.select { LegPlayers.leg eq legID }.map {
            it[LegPlayers.player].value to it[LegPlayers.playerTurnIndex]
        }.toMap()
    }
}
