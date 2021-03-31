package com.dartcaller

import com.dartcaller.dataClasses.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI
import javax.sql.DataSource

class Database (dataSource: DataSource?){
    private var database: Database? = null

    constructor() : this(null)

    init {
        database = when {
            dataSource != null -> {
                Database.connect(dataSource)
            }
            System.getenv("DATABASE_URL") is String -> {
                val (dbUrl, username, password) = this.destructDatabaseUrl(System.getenv("DATABASE_URL"))
                Database.connect(dbUrl, driver = "org.postgresql.Driver", user = username, password = password)
            }
            else -> throw IllegalArgumentException("Either a dataSource or a DatabaseUrl is needed")
        }
        transaction {
            SchemaUtils.create (Games, Players, Scores, Legs, GamePlayers, LegPlayers)
        }
    }

    private fun destructDatabaseUrl(uri: String): Triple<String, String, String> {
        val dbUri = URI(uri)
        val username = dbUri.userInfo.split(":")[0]
        val password = dbUri.userInfo.split(":")[1]
        val dbUrl = "jdbc:postgresql://" + dbUri.host + ':' + dbUri.port + dbUri.path + "?sslmode=require";
        return Triple(dbUrl, username, password)
    }
}
