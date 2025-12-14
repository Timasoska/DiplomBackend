package org.example.data.loader

import kotlinx.serialization.json.Json
import org.example.data.db.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object ContentLoader {

    fun loadFromResources(fileName: String = "/data/initial_content.json") {
        try {
            // 1. Читаем файл из ресурсов
            // Важно: в Docker (jar) файл лежит внутри classpath, поэтому getResourceAsStream надежнее
            val jsonStream = this::class.java.getResourceAsStream(fileName)

            if (jsonStream == null) {
                println("⚠️ Файл с данными $fileName не найден в ресурсах!")
                return
            }

            val jsonString = jsonStream.bufferedReader().use { it.readText() }

            // 2. Парсим JSON
            val disciplines = Json.decodeFromString<List<SeedDiscipline>>(jsonString)

            // 3. Пишем в базу
            transaction {
                if (!Disciplines.selectAll().empty()) {
                    println("ℹ️ База данных уже содержит данные. Пропуск загрузки.")
                    return@transaction
                }

                println("📦 Начинаем загрузку данных из JSON...")

                for (d in disciplines) {
                    // Создаем Дисциплину
                    val disciplineInsert = Disciplines.insert {
                        it[Disciplines.name] = d.name
                        it[Disciplines.description] = d.description
                    }
                    val disciplineId = disciplineInsert[Disciplines.id]

                    for (t in d.topics) {
                        // Создаем Тему
                        val topicInsert = Topics.insert {
                            it[Topics.name] = t.name
                            it[Topics.disciplineId] = disciplineId
                        }
                        val topicId = topicInsert[Topics.id]

                        // Создаем Лекции
                        for (l in t.lectures) {
                            Lectures.insert {
                                it[Lectures.title] = l.title
                                it[Lectures.content] = l.content
                                it[Lectures.topicId] = topicId
                            }
                        }

                        // Создаем Тест (если есть)
                        t.test?.let { test ->
                            val testInsert = Tests.insert {
                                it[Tests.title] = test.title
                                it[Tests.topicId] = topicId
                            }
                            val testId = testInsert[Tests.id]

                            for (q in test.questions) {
                                val qInsert = Questions.insert {
                                    it[Questions.questionText] = q.text
                                    it[Questions.difficulty] = q.difficulty // <--- Сохраняем в базу
                                    it[Questions.isMultipleChoice] = q.isMultipleChoice // <--- Сохраняем
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
                println("✅ Данные успешно загружены из JSON! (${disciplines.size} дисциплин)")
            }

        } catch (e: Exception) {
            println("❌ Ошибка при загрузке данных: ${e.message}")
            e.printStackTrace()
        }
    }
}