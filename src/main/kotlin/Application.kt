package org.example

import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch

// Импорты таблиц
import org.example.data.db.Disciplines
import org.example.data.db.Lectures
import org.example.data.db.Topics
import org.example.data.db.dbQuery
import org.example.di.appModule
import org.example.features.auth.authRouting
import org.example.features.content.contentRouting
import org.example.plugins.configureDatabases
import org.example.plugins.configureSecurity
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // 1. Настройка JSON
    install(ContentNegotiation) {
        json()
    }

    // 2. Настройка Koin (DI)
    install(Koin) {
        modules(appModule)
    }

    // 3. Подключение БД
    configureDatabases()

    configureSecurity()

    // 4. Заполнение базы
    launch {
        dbQuery {
            if (Disciplines.selectAll().empty()) {
                // --- 1. Создаем Дисциплину (Уголовное право) ---
                // Используем Disciplines.name вместо просто name
                val criminalLawId = Disciplines.insert {
                    it[Disciplines.name] = "Уголовное право"
                    it[Disciplines.description] = "Изучение преступлений и наказаний"
                } get Disciplines.id

                // --- 2. Создаем вторую дисциплину ---
                Disciplines.insert {
                    it[Disciplines.name] = "Гражданское право"
                    it[Disciplines.description] = "Регулирование отношений между гражданами"
                }

                // --- 3. Создаем Тему (привязываем к criminalLawId) ---
                val topicId = Topics.insert {
                    it[Topics.name] = "Понятие преступления"
                    it[Topics.disciplineId] = criminalLawId
                } get Topics.id

                // --- 4. Создаем Лекцию (привязываем к topicId) ---
                Lectures.insert {
                    it[Lectures.title] = "Что такое преступление?"
                    it[Lectures.content] = "Преступление — это виновно совершенное общественно опасное деяние, запрещенное настоящим Кодексом под угрозой наказания."
                    it[Lectures.topicId] = topicId
                }

                println("✅ База данных успешно заполнена контентом!")
            }
        }
    }

    // 5. Роутинг
    routing {
        contentRouting()
        authRouting()
    }
}