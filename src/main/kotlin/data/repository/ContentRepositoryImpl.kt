package org.example.data.repository

import org.example.data.db.*
import org.example.data.dto.*
import org.example.domain.model.*
import org.example.domain.repository.ContentRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.ResultSet
import java.time.LocalDateTime
import kotlin.text.Typography.quote

/**
 * Реализация репозитория данных.
 *
 * Архитектурное решение:
 * 1. Для CRUD-операций (чтение курсов, лекций, сохранение попыток) используется Exposed DSL.
 *    Это обеспечивает типобезопасность и защиту от SQL-инъекций.
 * 2. Для сложной аналитики (расчет трендов, агрегация) используется Native SQL (JDBC).
 *    Это позволяет использовать мощные функции PostgreSQL (REGR_SLOPE, Window Functions)
 *    и избежать выгрузки тысяч записей в оперативную память сервера.
 */
class ContentRepositoryImpl : ContentRepository {

    // --- PROGRESS (Закладки) ---
    override suspend fun saveLectureProgress(userId: Int, lectureId: Int, index: Int, quote: String?) = dbQuery {
        val existing = LectureProgress.select {
            (LectureProgress.userId eq userId) and (LectureProgress.lectureId eq lectureId)
        }.singleOrNull()

        if (existing != null) {
            LectureProgress.update({ (LectureProgress.userId eq userId) and (LectureProgress.lectureId eq lectureId) }) {
                it[LectureProgress.progressIndex] = index
                it[LectureProgress.selectedText] = quote
                it[LectureProgress.updatedAt] = LocalDateTime.now()
            }
        } else {
            LectureProgress.insert {
                it[LectureProgress.userId] = userId
                it[LectureProgress.lectureId] = lectureId
                it[LectureProgress.progressIndex] = index
                it[LectureProgress.selectedText] = quote
            }
        }
        Unit
    }

    override suspend fun getLectureProgress(userId: Int, lectureId: Int): LectureProgressDto? = dbQuery {
        LectureProgress.select {
            (LectureProgress.userId eq userId) and (LectureProgress.lectureId eq lectureId)
        }.map {
            LectureProgressDto(
                lectureId = it[LectureProgress.lectureId],
                progressIndex = it[LectureProgress.progressIndex],
                quote = it[LectureProgress.selectedText]
            )
        }.singleOrNull()
    }

    override suspend fun getLeaderboard(): List<LeaderboardItemDto> = dbQuery {
        // Считаем рейтинг: (Кол-во тестов * Средний балл)
        // Сортируем от большего к меньшему
        // Берем Топ-10
        val sql = """
            SELECT 
                u.email,
                COUNT(ta.test_id) as tests_count,
                COALESCE(AVG(ta.score), 0) as avg_score
            FROM users u
            JOIN test_attempts ta ON u.user_id = ta.user_id
            GROUP BY u.user_id, u.email
            ORDER BY (COUNT(ta.test_id) * AVG(ta.score)) DESC
            LIMIT 10;
        """.trimIndent()

        val leaderboard = mutableListOf<LeaderboardItemDto>()

        val jdbcConnection = (connection.connection as java.sql.Connection)
        val stmt = jdbcConnection.prepareStatement(sql)
        val rs = stmt.executeQuery()

        while (rs.next()) {
            val count = rs.getInt("tests_count")
            val avg = rs.getDouble("avg_score")
            val totalScore = count * avg

            leaderboard.add(
                LeaderboardItemDto(
                    email = rs.getString("email"),
                    score = String.format("%.1f", totalScore).replace(',', '.').toDouble(),
                    testsPassed = count
                )
            )
        }
        stmt.close()

        leaderboard
    }

    // --- АНАЛИТИКА (Native SQL) ---

    override suspend fun getUserTestResults(userId: Int): List<Pair<Int, Int>> = dbQuery {
        val sql = """
            SELECT t.topic_id, ta.score 
            FROM test_attempts ta
            JOIN tests t ON ta.test_id = t.test_id
            WHERE ta.user_id = ?
            ORDER BY ta.attempted_at ASC
        """.trimIndent()

        val results = mutableListOf<Pair<Int, Int>>()

        // Используем безопасный PreparedStatement через текущее соединение транзакции
        execPattern(sql, listOf(userId)) { rs ->
            while (rs.next()) {
                results.add(rs.getInt("topic_id") to rs.getInt("score"))
            }
        }
        results
    }

    override suspend fun getFullProgress(userId: Int): ProgressDto = dbQuery {
        // 1. Расчет общей статистики (Всего сдано, Средний балл, Общий тренд)
        // Используем CTE и оконные функции PostgreSQL
        val globalSql = """
            WITH ordered_attempts AS (
                SELECT 
                    CAST(score AS FLOAT) as score_float, 
                    CAST(ROW_NUMBER() OVER (ORDER BY attempted_at) AS FLOAT) as rn
                FROM test_attempts
                WHERE user_id = ?
            )
            SELECT 
                COUNT(*) as total_count,
                COALESCE(AVG(score_float), 0) as avg_score,
                COALESCE(REGR_SLOPE(score_float, rn), 0) as trend
            FROM ordered_attempts;
        """.trimIndent()

        var totalTests = 0
        var totalAvg = 0.0
        var totalTrend = 0.0

        execPattern(globalSql, listOf(userId)) { rs ->
            if (rs.next()) {
                totalTests = rs.getInt("total_count")
                totalAvg = rs.getDouble("avg_score")
                totalTrend = rs.getDouble("trend")
            }
        }

        // 2. Расчет статистики по каждой дисциплине
        val disciplinesSql = """
            WITH ordered_attempts AS (
                SELECT 
                    d.name as discipline_name,
                    d.discipline_id as discipline_id,
                    CAST(ta.score AS FLOAT) as score_float,
                    CAST(ROW_NUMBER() OVER (PARTITION BY d.discipline_id ORDER BY ta.attempted_at) AS FLOAT) as rn
                FROM test_attempts ta
                JOIN tests t ON ta.test_id = t.test_id
                JOIN topics top ON t.topic_id = top.topic_id
                JOIN disciplines d ON top.discipline_id = d.discipline_id
                WHERE ta.user_id = ?
            )
            SELECT 
                discipline_id,
                discipline_name,
                COALESCE(AVG(score_float), 0) as avg_score,
                COALESCE(REGR_SLOPE(score_float, rn), 0) as trend
            FROM ordered_attempts
            GROUP BY discipline_id, discipline_name;
        """.trimIndent()

        val disciplinesStats = mutableListOf<DisciplineStatDto>()

        execPattern(disciplinesSql, listOf(userId)) { rs ->
            while (rs.next()) {
                disciplinesStats.add(
                    DisciplineStatDto(
                        id = rs.getInt("discipline_id"),
                        name = rs.getString("discipline_name"),
                        averageScore = String.format("%.1f", rs.getDouble("avg_score")).replace(',', '.').toDouble(),
                        trend = String.format("%.2f", rs.getDouble("trend")).replace(',', '.').toDouble()
                    )
                )
            }
        }

        // --- НОВЫЙ БЛОК: ПОЛУЧЕНИЕ ИСТОРИИ (ДЛЯ ГРАФИКА) ---
        val historySql = "SELECT score FROM test_attempts WHERE user_id = ? ORDER BY attempted_at ASC LIMIT 20"
        val history = mutableListOf<Int>()

        val stmtHistory = (connection.connection as java.sql.Connection).prepareStatement(historySql)
        stmtHistory.setInt(1, userId)
        val rsHistory = stmtHistory.executeQuery()

        while (rsHistory.next()) {
            history.add(rsHistory.getInt("score"))
        }
        stmtHistory.close()
        // ---------------------------------------------------

        ProgressDto(
            testsPassed = totalTests,
            averageScore = String.format("%.1f", totalAvg).replace(',', '.').toDouble(),
            trend = String.format("%.2f", totalTrend).replace(',', '.').toDouble(),
            history = history, // <--- Передаем историю
            disciplines = disciplinesStats
        )
    }

    // Вспомогательная функция для безопасного выполнения SELECT через JDBC
    private fun <T> Transaction.execPattern(sql: String, params: List<Any>, transform: (ResultSet) -> T): T? {
        // Достаем "голое" JDBC соединение
        val jdbcConnection = (connection.connection as java.sql.Connection)

        // Создаем PreparedStatement
        val stmt = jdbcConnection.prepareStatement(sql)

        params.forEachIndexed { index, value ->
            // Устанавливаем параметры (индекс в JDBC начинается с 1)
            when (value) {
                is Int -> stmt.setInt(index + 1, value)
                is String -> stmt.setString(index + 1, value)
                // Если будут Double или другие типы - добавь их сюда
            }
        }

        val rs = stmt.executeQuery()
        val result = transform(rs)

        // Закрываем ресурсы
        stmt.close()

        return result
    }


    // --- CRUD ОПЕРАЦИИ (Exposed DSL) ---

    override suspend fun getAllDisciplines(): List<Discipline> = dbQuery {
        Disciplines.selectAll().map { row ->
            Discipline(
                id = row[Disciplines.id],
                name = row[Disciplines.name],
                description = row[Disciplines.description]
            )
        }
    }

    override suspend fun getTopicsByDisciplineId(disciplineId: Int): List<Topic> = dbQuery {
        Topics.select { Topics.disciplineId eq disciplineId }
            .map { row ->
                Topic(
                    id = row[Topics.id],
                    name = row[Topics.name],
                    disciplineId = row[Topics.disciplineId]
                )
            }
    }

    override suspend fun getLectureByTopicId(topicId: Int): List<Lecture> = dbQuery {
        Lectures.select { Lectures.topicId eq topicId }
            .map { row ->
                Lecture(
                    id = row[Lectures.id],
                    title = row[Lectures.title],
                    content = row[Lectures.content],
                    topicId = row[Lectures.topicId]
                )
            }
    }

    override suspend fun getLectureById(lectureId: Int, userId: Int): LectureDto? = dbQuery {
        val lectureRow = Lectures.select { Lectures.id eq lectureId }.singleOrNull()
            ?: return@dbQuery null

        // Проверяем, есть ли лайк от этого пользователя
        val isFavorite = UserFavorites.select {
            (UserFavorites.lectureId eq lectureId) and (UserFavorites.userId eq userId)
        }.count() > 0

        LectureDto(
            id = lectureRow[Lectures.id],
            title = lectureRow[Lectures.title],
            content = lectureRow[Lectures.content],
            topicId = lectureRow[Lectures.topicId],
            isFavorite = isFavorite // <--- Заполняем поле
        )
    }

    override suspend fun addFavorite(userId: Int, lectureId: Int) = dbQuery {
        // Проверяем на дубликаты перед вставкой
        val exists = UserFavorites.select {
            (UserFavorites.userId eq userId) and (UserFavorites.lectureId eq lectureId)
        }.count() > 0

        if (!exists) {
            UserFavorites.insert {
                it[UserFavorites.userId] = userId
                it[UserFavorites.lectureId] = lectureId
            }
        }
    }

    override suspend fun removeFavorite(userId: Int, lectureId: Int) = dbQuery {
        UserFavorites.deleteWhere {
            (UserFavorites.userId eq userId) and (UserFavorites.lectureId eq lectureId)
        }
        Unit
    }

    override suspend fun getFavorites(userId: Int): List<Lecture> = dbQuery {
        (Lectures innerJoin UserFavorites)
            .select { UserFavorites.userId eq userId }
            .map { row ->
                Lecture(
                    id = row[Lectures.id],
                    title = row[Lectures.title],
                    content = row[Lectures.content],
                    topicId = row[Lectures.topicId],
                    isFavorite = true // <--- В списке избранного это всегда true!
                )
            }
    }

    override suspend fun searchLectures(query: String): List<Lecture> = dbQuery {
        val searchQuery = "%${query.lowercase()}%"
        Lectures.select {
            (Lectures.title.lowerCase() like searchQuery) or
                    (Lectures.content.lowerCase() like searchQuery)
        }.map { row ->
            Lecture(
                id = row[Lectures.id],
                title = row[Lectures.title],
                content = row[Lectures.content],
                topicId = row[Lectures.topicId]
            )
        }
    }

    override suspend fun getTestByTopicId(topicId: Int): Test? = dbQuery {
        val testRow = Tests.select { Tests.topicId eq topicId }.singleOrNull() ?: return@dbQuery null
        val testId = testRow[Tests.id]

        val questions = Questions.select { Questions.testId eq testId }.map { qRow ->
            val qId = qRow[Questions.id]
            val answers = Answers.select { Answers.questionId eq qId }.map { aRow ->
                Answer(
                    id = aRow[Answers.id],
                    text = aRow[Answers.answerText],
                    isCorrect = aRow[Answers.isCorrect]
                )
            }
            Question(
                id = qId,
                text = qRow[Questions.questionText],
                difficulty = qRow[Questions.difficulty],
                isMultipleChoice = qRow[Questions.isMultipleChoice], // <--- Читаем из БД
                answers = answers
            )
        }

        Test(
            id = testId,
            title = testRow[Tests.title],
            topicId = testRow[Tests.topicId],
            timeLimit = testRow[Tests.timeLimit], // <--- Читаем таймер
            questions = questions
        )
    }

    override suspend fun saveTestAttempt(userId: Int, testId: Int, score: Int) = dbQuery {
        TestAttempts.insert {
            it[TestAttempts.userId] = userId
            it[TestAttempts.testId] = testId
            it[TestAttempts.score] = score
        }
        Unit
    }

    override suspend fun getCorrectAnswers(testId: Int): Map<Int, List<Int>> = dbQuery {
        val result = mutableMapOf<Int, MutableList<Int>>()

        // Джойним Ответы с Вопросами, фильтруем по ID теста и правильности
        val query = (Answers innerJoin Questions)
            .slice(Answers.questionId, Answers.id)
            .select { (Questions.testId eq testId) and (Answers.isCorrect eq true) }

        query.forEach { row ->
            val qId = row[Answers.questionId]
            val aId = row[Answers.id]
            result.computeIfAbsent(qId) { mutableListOf() }.add(aId)
        }

        result
    }

    override suspend fun importContent(data: List<org.example.data.loader.SeedDiscipline>) = dbQuery {
        // Мы используем ту же логику, что была в ContentLoader, но теперь она доступна через API
        for (d in data) {
            // 1. Дисциплина
            val disciplineId = Disciplines.insert {
                it[Disciplines.name] = d.name
                it[Disciplines.description] = d.description
            } get Disciplines.id

            for (t in d.topics) {
                // 2. Тема
                val topicId = Topics.insert {
                    it[Topics.name] = t.name
                    it[Topics.disciplineId] = disciplineId
                } get Topics.id

                // 3. Лекции
                for (l in t.lectures) {
                    Lectures.insert {
                        it[Lectures.title] = l.title
                        it[Lectures.content] = l.content
                        it[Lectures.topicId] = topicId
                    }
                }

                // 4. Тест (если есть)
                t.test?.let { test ->
                    val testId = Tests.insert {
                        it[Tests.title] = test.title
                        it[Tests.topicId] = topicId
                        it[Tests.timeLimit] = test.timeLimit
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
        }
    }
}