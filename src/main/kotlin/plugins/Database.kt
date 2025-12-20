package org.example.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.example.data.db.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val config = HikariConfig().apply {
        driverClassName = environment.config.property("storage.driverClassName").getString()
        jdbcUrl = environment.config.property("storage.jdbcURL").getString()
        username = environment.config.property("storage.user").getString()
        password = environment.config.property("storage.password").getString()
        maximumPoolSize = 3
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    val database = Database.connect(HikariDataSource(config))

    transaction(database) {
        SchemaUtils.create(
            Users, Disciplines, Topics, Lectures,
            Tests, Questions, Answers, TestAttempts, UserFavorites,
            LectureProgress,
            LectureFiles,       // <--- ВОТ ЭТО
            StudentGroups,
            GroupMembers,
            FlashcardProgress // <--- ДОБАВЛЕНА НОВАЯ ТАБЛИЦА
        )
    }
}