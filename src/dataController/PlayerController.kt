package com.dartcaller.dataController

import com.dartcaller.dataClasses.Player
import org.jetbrains.exposed.sql.transactions.transaction

object PlayerController {
    fun getAll(): List<Player> {
        return transaction {
            Player.all().toList()
        }
    }

    fun create(name: String): Player {
        return transaction {
            Player.new { this.name = name }
        }
    }
}
