package com.efetepe.amigos.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

object DatabaseFactory {
    private const val DB_NAME = "amigos.db"

    fun create(): AmigosDatabase {
        val dbDir = File(System.getProperty("user.home"), ".amigos")
        dbDir.mkdirs()
        val dbPath = File(dbDir, DB_NAME).absolutePath

        val driver = JdbcSqliteDriver("jdbc:sqlite:$dbPath")

        // Only create schema if tables don't exist yet
        try {
            AmigosDatabase.Schema.create(driver)
        } catch (_: Exception) {
            // Tables already exist — this is a subsequent launch
        }

        return AmigosDatabase(driver)
    }

    fun createInMemory(): AmigosDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AmigosDatabase.Schema.create(driver)
        return AmigosDatabase(driver)
    }
}
