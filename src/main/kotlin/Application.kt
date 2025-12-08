package org.example

import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import org.example.data.db.*

// Импорты таблиц
import org.example.di.appModule
import org.example.features.auth.authRouting
import org.example.features.content.contentRouting
import org.example.features.testing.testingRouting
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

                val testId = Tests.insert {
                    it[Tests.title] = "Тест по теме: Понятие преступления"
                    it[Tests.topicId] = topicId
                } get Tests.id

                // Вопрос 1
                val q1 = Questions.insert {
                    it[Questions.questionText] = "Является ли преступлением мысль о краже?"
                    it[Questions.testId] = testId
                } get Questions.id

                Answers.insert { it[answerText] = "Да"; it[isCorrect] = false; it[questionId] = q1 }
                Answers.insert { it[answerText] = "Нет"; it[isCorrect] = true; it[questionId] = q1 } // Правильный

                // Вопрос 2
                val q2 = Questions.insert {
                    it[Questions.questionText] = "Какой признак не относится к преступлению?"
                    it[Questions.testId] = testId
                } get Questions.id

                Answers.insert { it[answerText] = "Виновность"; it[isCorrect] = false; it[questionId] = q2 }
                Answers.insert { it[answerText] = "Общественная опасность"; it[isCorrect] = false; it[questionId] = q2 }
                Answers.insert { it[answerText] = "Полезность"; it[isCorrect] = true; it[questionId] = q2 } // Правильный

                println("✅ База данных успешно заполнена контентом!")
            }
        }
    }

    // 5. Роутинг
    routing {
        contentRouting()
        authRouting()
        testingRouting()
    }
}