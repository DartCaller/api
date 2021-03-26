package com.dartcaller.dataClasses

import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object Players: UUIDTable() {
    val name = varchar("name", 50)
}

class Player(
    val id: UUID,
    val name: String
)
