package com.dartcaller.dataClasses

import org.jetbrains.exposed.sql.Table

object LegPlayers: Table() {
    val leg = reference("leg", Legs)
    val player = reference("player", Players)
    val playerTurnIndex = integer("playerTurnIndex")
    override val primaryKey = PrimaryKey(leg, player)
}
