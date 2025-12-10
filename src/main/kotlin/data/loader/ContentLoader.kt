package org.example.data.loader

import kotlinx.serialization.json.Json
import org.example.data.db.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object ContentLoader {

    fun loadFromResources(fileName: String = "/data/initial_content.json") {
        try {
            val jsonStream = this::class.java.getResourceAsStream(fileName)

            if (jsonStream == null) {
                println("⚠️ Файл с данными $fileName не найден!")
                return
            }

            val jsonString = jsonStream.bufferedReader().use { it.readText() }
            val disciplines = Json.decodeFromString<List<SeedDiscipline>>(jsonString)

            transaction {
                // Если в базе уже есть дисциплины - не дублируем
                if (!Disciplines.selectAll().empty()) {
                    println("ℹ️ База данных уже заполнена. Пропуск загрузки.")
                    return@transaction
                }

                println("📦 Начинаем загрузку контента из JSON...")

                for (d in disciplines) {
                    // 1. Дисциплина
                    val disciplineInsert = Disciplines.insert {
                        it[Disciplines.name] = d.name
                        it[Disciplines.description] = d.description
                    }
                    val disciplineId = disciplineInsert[Disciplines.id]

                    for (t in d.topics) {
                        // 2. Тема
                        val topicInsert = Topics.insert {
                            it[Topics.name] = t.name
                            it[Topics.disciplineId] = disciplineId
                        }
                        val topicId = topicInsert[Topics.id]

                        // 3. Лекции
                        for (l in t.lectures) {
                            Lectures.insert {
                                it[Lectures.title] = l.title
                                it[Lectures.content] = l.content
                                it[Lectures.topicId] = topicId
                            }
                        }

                        // 4. Тест
                        t.test?.let { test ->
                            val testInsert = Tests.insert {
                                it[Tests.title] = test.title
                                it[Tests.topicId] = topicId
                            }
                            val testId = testInsert[Tests.id]

                            for (q in test.questions) {
                                val qInsert = Questions.insert {
                                    it[Questions.questionText] = q.text
                                    it[Questions.testId] = testId
                                }
                                val qId = qInsert[Questions.id]

                                for (a in q.answers) {
                                    Answers.insert {
                                        it[Answers.answerText] = a.text
                                        it[Answers.isCorrect] = a.isCorrect
                                        it[Answers.questionId] = qId
                                    }
                                }
                            }
                        }
                    }
                }
                println("✅ Контент успешно загружен! (${disciplines.size} дисциплин)")
            }

        } catch (e: Exception) {
            println("❌ Ошибка при загрузке JSON: ${e.message}")
            e.printStackTrace()
        }
    }
}