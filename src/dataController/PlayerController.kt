package com.dartcaller.dataController

import com.dartcaller.dataClasses.LegPlayers
import com.dartcaller.dataClasses.Player
import com.dartcaller.dataClasses.Players
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object PlayerController {
    fun create(name: String): Player {
        val newPlayerID = Players.insertAndGetId {
            it[Players.name] = name
        }
        return Player(newPlayerID.value, name)
    }

    fun getByLeg(legID: UUID): List<Player> {
        return transaction {
            (LegPlayers.innerJoin(Players))
                .slice(Players.id, Players.name)
                .select { LegPlayers.leg eq legID }
                .map {
                    Player(
                        it[Players.id].value,
                        it[Players.name]
                    )
                }
        }
    }
}
