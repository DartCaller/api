package com.dartcaller.dataController

import com.dartcaller.dataClasses.GamePlayers
import org.jetbrains.exposed.sql.insert
import java.util.*

object GamePlayerController {
    fun create(game: UUID, player: UUID) {
        GamePlayers.insert {
            it[this.game] = game
            it[this.player] = player
        }
    }
}
