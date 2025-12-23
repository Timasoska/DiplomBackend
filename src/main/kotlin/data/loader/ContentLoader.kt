package org.example.data.loader

import kotlinx.serialization.json.Json
import org.example.data.db.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.InputStream

object ContentLoader {

    // Список файлов для загрузки
    private val dataFiles = listOf(
        "land_law.json",
        "land_law7-10.json",
        "land_law11-14.json",
        "land_law15-20.json",
    )

    fun loadAllContent() {
        transaction {
            println("📦 [LOADER] Проверка контента...")

            for (fileName in dataFiles) {
                loadSingleFile("/data/$fileName")
            }

            println("✅ [LOADER] Синхронизация завершена.")
        }
    }

    private fun loadSingleFile(filePath: String) {
        try {
            val jsonStream: InputStream? = this::class.java.getResourceAsStream(filePath)

            if (jsonStream == null) {
                println("⚠️ [LOADER] Файл $filePath не найден!")
                return
            }

            val jsonString = jsonStream.bufferedReader().use { it.readText() }
            val disciplines = Json.decodeFromString<List<SeedDiscipline>>(jsonString)

            for (d in disciplines) {
                // 1. Ищем ID дисциплины по имени
                var disciplineId = Disciplines
                    .slice(Disciplines.id)
                    .select { Disciplines.name eq d.name }
                    .singleOrNull()
                    ?.get(Disciplines.id)

                // 2. Если дисциплины нет — создаем
                if (disciplineId == null) {
                    println("   -> [NEW] Создание дисциплины: '${d.name}'")
                    disciplineId = Disciplines.insert {
                        it[Disciplines.name] = d.name
                        it[Disciplines.description] = d.description
                    } get Disciplines.id
                } else {
                    println("   -> [UPDATE] Дисциплина '${d.name}' найдена (ID: $disciplineId). Добавляем новые темы...")
                }

                // 3. Загружаем темы (с проверкой на дубликаты)
                for (t in d.topics) {
                    // Исправлено: Используем .and() для надежности и безопасный вызов disciplineId
                    val currentDisciplineId = disciplineId!!

                    val topicExists = Topics.select {
                        (Topics.name eq t.name).and(Topics.disciplineId eq currentDisciplineId)
                    }.count() > 0

                    if (topicExists) {
                        // print(".")
                        continue
                    }

                    val topicId = Topics.insert {
                        it[Topics.name] = t.name
                        it[Topics.disciplineId] = currentDisciplineId
                    } get Topics.id

                    // Лекции
                    for (l in t.lectures) {
                        val lectureId = Lectures.insert {
                            it[Lectures.title] = l.title
                            it[Lectures.content] = l.content
                            it[Lectures.topicId] = topicId
                        } get Lectures.id

                        l.test?.let { test ->
                            insertTest(test, topicId = null, lectureId = lectureId)
                        }
                    }

                    // Тест по теме
                    t.test?.let { test ->
                        insertTest(test, topicId = topicId, lectureId = null)
                    }
                }
                println("      ✅ Темы из файла $filePath успешно обработаны.")
            }

        } catch (e: Exception) {
            println("❌ [LOADER] Ошибка в файле $filePath: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun insertTest(test: SeedTest, topicId: Int?, lectureId: Int?) {
        val testId = Tests.insert {
            it[Tests.title] = test.title
            it[Tests.timeLimit] = test.timeLimit
            it[Tests.topicId] = topicId
            it[Tests.lectureId] = lectureId
        } get Tests.id

        for (q in test.questions) {
            val qId = Questions.insert {
                it[questionText] = q.text
                it[difficulty] = q.difficulty
                it[isMultipleChoice] = q.isMultipleChoice
                it[Questions.testId] = testId
            } get Questions.id

            for (a in q.answers) {
                Answers.insert {
                    it[answerText] = a.text
                    it[isCorrect] = a.isCorrect
                    it[questionId] = qId
                }
            }
        }
    }
}