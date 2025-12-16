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
                    val disciplineId = Disciplines.insert {
                        it[Disciplines.name] = d.name
                        it[Disciplines.description] = d.description
                    } get Disciplines.id

                    for (t in d.topics) {
                        val topicId = Topics.insert {
                            it[Topics.name] = t.name
                            it[Topics.disciplineId] = disciplineId
                        } get Topics.id

                        // ЛЕКЦИИ
                        for (l in t.lectures) {
                            val lectureId = Lectures.insert {
                                it[Lectures.title] = l.title
                                it[Lectures.content] = l.content
                                it[Lectures.topicId] = topicId
                            } get Lectures.id

                            // --- ТЕСТ ПО ЛЕКЦИИ (Если есть) ---
                            l.test?.let { test ->
                                insertTest(test, topicId = null, lectureId = lectureId)
                            }
                        }

                        // --- ТЕСТ ПО ТЕМЕ (Если есть) ---
                        t.test?.let { test ->
                            insertTest(test, topicId = topicId, lectureId = null)
                        }
                    }
                }
                println("✅ Данные успешно загружены из JSON!")
            }

        } catch (e: Exception) {
            println("❌ Ошибка при загрузке данных: ${e.message}")
            e.printStackTrace()
        }
    }
    // Вспомогательная функция, чтобы не дублировать код вставки вопросов
    private fun insertTest(test: org.example.data.loader.SeedTest, topicId: Int?, lectureId: Int?) {
        val testId = Tests.insert {
            it[Tests.title] = test.title
            it[Tests.timeLimit] = test.timeLimit
            it[Tests.topicId] = topicId
            it[Tests.lectureId] = lectureId
        } get Tests.id

        for (q in test.questions) {
            val qId = Questions.insert {
                it[Questions.questionText] = q.text
                it[Questions.difficulty] = q.difficulty
                it[Questions.isMultipleChoice] = q.isMultipleChoice
                it[Questions.testId] = testId
            } get Questions.id

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