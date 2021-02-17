package com.dartcaller.dataClasses

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object Players: UUIDTable() {
    val name = varchar("name", 50)
}

class Player(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<Player>(Players)
    var name by Players.name
}
