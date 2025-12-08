package org.example

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import org.example.data.db.*
import org.example.di.appModule
import org.example.features.auth.authRouting
import org.example.features.content.contentRouting
import org.example.features.testing.testingRouting
import org.example.features.analytics.analyticsRouting
import org.example.plugins.configureDatabases
import org.example.plugins.configureSecurity
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(Koin) {
        modules(appModule)
    }

    configureDatabases()
    configureSecurity()

    // Заполнение базы данными
    launch {
        dbQuery {
            if (Disciplines.selectAll().empty()) {
                println("🚀 Начинаем заполнение базы данных...")

                // 1. Дисциплины
                val criminalLawInsert = Disciplines.insert {
                    it[Disciplines.name] = "Уголовное право"
                    it[Disciplines.description] = "Изучение преступлений и наказаний"
                }
                val criminalLawId = criminalLawInsert[Disciplines.id]

                Disciplines.insert {
                    it[Disciplines.name] = "Гражданское право"
                    it[Disciplines.description] = "Регулирование отношений между гражданами"
                }

                // 2. Темы
                val topicInsert = Topics.insert {
                    it[Topics.name] = "Понятие преступления"
                    it[Topics.disciplineId] = criminalLawId
                }
                val topicId = topicInsert[Topics.id]

                // 3. Лекции
                Lectures.insert {
                    it[Lectures.title] = "Что такое преступление?"
                    it[Lectures.content] = "Преступление — это виновно совершенное общественно опасное деяние..."
                    it[Lectures.topicId] = topicId
                }

                // --- 4. Тесты (ИСПРАВЛЕНО: Добавлены явные указания таблиц Tests.) ---
                val testInsert = Tests.insert {
                    it[Tests.title] = "Тест: Понятие преступления"
                    it[Tests.topicId] = topicId
                }
                val testId = testInsert[Tests.id]

                // Вопрос 1 (ИСПРАВЛЕНО: Questions.)
                val q1Insert = Questions.insert {
                    it[Questions.questionText] = "Является ли мысль о преступлении преступлением?"
                    it[Questions.testId] = testId
                }
                val q1Id = q1Insert[Questions.id]

                // Ответы 1 (ИСПРАВЛЕНО: Answers.)
                Answers.insert {
                    it[Answers.answerText] = "Да"
                    it[Answers.isCorrect] = false
                    it[Answers.questionId] = q1Id
                }
                Answers.insert {
                    it[Answers.answerText] = "Нет"
                    it[Answers.isCorrect] = true
                    it[Answers.questionId] = q1Id
                }

                // Вопрос 2 (ИСПРАВЛЕНО: Questions.)
                val q2Insert = Questions.insert {
                    it[Questions.questionText] = "Обязательный признак преступления?"
                    it[Questions.testId] = testId
                }
                val q2Id = q2Insert[Questions.id]

                // Ответы 2 (ИСПРАВЛЕНО: Answers.)
                Answers.insert {
                    it[Answers.answerText] = "Красота"
                    it[Answers.isCorrect] = false
                    it[Answers.questionId] = q2Id
                }
                Answers.insert {
                    it[Answers.answerText] = "Общественная опасность"
                    it[Answers.isCorrect] = true
                    it[Answers.questionId] = q2Id
                }

                println("✅ База данных успешно заполнена!")
            }
        }
    }

    routing {
        contentRouting()
        authRouting()
        testingRouting()
        analyticsRouting()
    }
}