package org.example.data.repository

import org.example.data.db.*
import org.example.data.dto.*
import org.example.data.loader.SeedDiscipline
import org.example.domain.model.*
import org.example.domain.repository.ContentRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import java.sql.Connection
import java.sql.ResultSet
import java.time.LocalDateTime

/**
 * Реализация репозитория данных (Backend).
 */
class ContentRepositoryImpl : ContentRepository {

    // --- ПОЛУЧЕНИЕ ПОЛНОГО ТЕСТА (ДЛЯ УЧИТЕЛЯ) ---

    override suspend fun getFullTestByLectureId(lectureId: Int): AdminTestResponse? = dbQuery {
        val testRow = Tests.select { Tests.lectureId eq lectureId }.singleOrNull()
            ?: return@dbQuery null

        val testId = testRow[Tests.id]

        val questions = Questions.select { Questions.testId eq testId }.map { qRow ->
            val qId = qRow[Questions.id]
            val answers = Answers.select { Answers.questionId eq qId }.map { aRow ->
                SaveAnswerRequest(
                    text = aRow[Answers.answerText],
                    isCorrect = aRow[Answers.isCorrect]
                )
            }
            SaveQuestionRequest(
                text = qRow[Questions.questionText],
                difficulty = qRow[Questions.difficulty],
                isMultipleChoice = qRow[Questions.isMultipleChoice],
                answers = answers
            )
        }

        AdminTestResponse(
            id = testId,
            title = testRow[Tests.title],
            topicId = testRow[Tests.topicId],
            lectureId = testRow[Tests.lectureId],
            timeLimit = testRow[Tests.timeLimit],
            questions = questions
        )
    }

    override suspend fun getFullTestByTopicId(topicId: Int): AdminTestResponse? = dbQuery {
        val testRow = Tests.select { Tests.topicId eq topicId }.singleOrNull() ?: return@dbQuery null
        val testId = testRow[Tests.id]

        val questions = Questions.select { Questions.testId eq testId }.map { qRow ->
            val qId = qRow[Questions.id]
            val answers = Answers.select { Answers.questionId eq qId }.map { aRow ->
                SaveAnswerRequest(
                    text = aRow[Answers.answerText],
                    isCorrect = aRow[Answers.isCorrect]
                )
            }
            SaveQuestionRequest(
                text = qRow[Questions.questionText],
                difficulty = qRow[Questions.difficulty],
                isMultipleChoice = qRow[Questions.isMultipleChoice],
                answers = answers
            )
        }

        AdminTestResponse(
            id = testId,
            title = testRow[Tests.title],
            topicId = testRow[Tests.topicId],
            lectureId = testRow[Tests.lectureId], // <--- ДОБАВЛЕНО (было пропущено)
            timeLimit = testRow[Tests.timeLimit],
            questions = questions
        )
    }

    // --- ПОЛУЧЕНИЕ ТЕСТА (ДЛЯ СТУДЕНТА) ---

    override suspend fun getTestByLectureId(lectureId: Int): Test? = dbQuery {
        val testRow = Tests.select { Tests.lectureId eq lectureId }.singleOrNull() ?: return@dbQuery null
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
                isMultipleChoice = qRow[Questions.isMultipleChoice],
                answers = answers
            )
        }

        Test(
            id = testId,
            title = testRow[Tests.title],
            topicId = testRow[Tests.topicId],
            lectureId = testRow[Tests.lectureId],
            timeLimit = testRow[Tests.timeLimit],
            questions = questions
        )
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
                isMultipleChoice = qRow[Questions.isMultipleChoice],
                answers = answers
            )
        }

        Test(
            id = testId,
            title = testRow[Tests.title],
            topicId = testRow[Tests.topicId],
            lectureId = testRow[Tests.lectureId],
            timeLimit = testRow[Tests.timeLimit],
            questions = questions
        )
    }

    // --- СОХРАНЕНИЕ ТЕСТА (УЧИТЕЛЬ) ---

    override suspend fun saveTest(request: SaveTestRequest) = dbQuery {
        // 1. Ищем существующий тест (по TopicId ИЛИ по LectureId)
        val existingTest = if (request.topicId != null) {
            Tests.select { Tests.topicId eq request.topicId }.singleOrNull()
        } else if (request.lectureId != null) {
            Tests.select { Tests.lectureId eq request.lectureId }.singleOrNull()
        } else {
            null
        }

        if (existingTest != null) {
            val testId = existingTest[Tests.id]

            // Удаляем старый тест (статистику, ответы, вопросы, сам тест)
            TestAttempts.deleteWhere { TestAttempts.testId eq testId }

            val questionIds = Questions.select { Questions.testId eq testId }.map { it[Questions.id] }
            if (questionIds.isNotEmpty()) {
                Answers.deleteWhere { questionId inList questionIds }
            }
            Questions.deleteWhere { Questions.testId eq testId }
            Tests.deleteWhere { id eq testId }
        }

        // 2. Создаем новый тест
        val newTestId = Tests.insert {
            it[title] = request.title
            it[timeLimit] = request.timeLimit
            it[topicId] = request.topicId
            it[lectureId] = request.lectureId
        } get Tests.id

        // 3. Сохраняем вопросы
        for (q in request.questions) {
            val qId = Questions.insert {
                it[questionText] = q.text
                it[testId] = newTestId
                it[difficulty] = q.difficulty
                it[isMultipleChoice] = q.isMultipleChoice
            } get Questions.id

            for (a in q.answers) {
                Answers.insert {
                    it[answerText] = a.text
                    it[questionId] = qId
                    it[isCorrect] = a.isCorrect
                }
            }
        }
        Unit
    }

    // --- ИМПОРТ ДАННЫХ (ADMIN / JSON) ---

    override suspend fun importContent(data: List<SeedDiscipline>) = dbQuery {
        for (d in data) {
            // 1. Дисциплина
            val disciplineId = Disciplines.insert {
                it[name] = d.name
                it[description] = d.description
            } get Disciplines.id

            for (t in d.topics) {
                // 2. Тема
                val topicId = Topics.insert {
                    it[name] = t.name
                    it[Topics.disciplineId] = disciplineId
                } get Topics.id

                // 3. Лекции
                for (l in t.lectures) {
                    val lectureId = Lectures.insert {
                        it[title] = l.title
                        it[content] = l.content
                        it[Lectures.topicId] = topicId
                    } get Lectures.id

                    // 4а. ТЕСТ ПО ЛЕКЦИИ (НОВОЕ)
                    l.test?.let { test ->
                        insertTestInternal(test, topicId = null, lectureId = lectureId)
                    }
                }

                // 4б. ТЕСТ ПО ТЕМЕ
                t.test?.let { test ->
                    insertTestInternal(test, topicId = topicId, lectureId = null)
                }
            }
        }
    }

    // Вспомогательный метод для вставки теста (используется при импорте)
    private fun insertTestInternal(test: org.example.data.loader.SeedTest, topicId: Int?, lectureId: Int?) {
        val testId = Tests.insert {
            it[title] = test.title
            it[Tests.topicId] = topicId
            it[Tests.lectureId] = lectureId
            it[timeLimit] = test.timeLimit
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

    // --- ОСТАЛЬНЫЕ МЕТОДЫ (Без изменений) ---

    override suspend fun deleteLecture(id: Int) = dbQuery {
        UserFavorites.deleteWhere { lectureId eq id }
        LectureProgress.deleteWhere { lectureId eq id }
        Lectures.deleteWhere { Lectures.id eq id }
        Unit
    }

    override suspend fun updateLecture(id: Int, title: String, content: String) = dbQuery {
        Lectures.update({ Lectures.id eq id }) {
            it[Lectures.title] = title
            it[Lectures.content] = content
        }
        Unit
    }

    override suspend fun saveLectureProgress(userId: Int, lectureId: Int, index: Int, quote: String?) = dbQuery {
        val existing = LectureProgress.select {
            (LectureProgress.userId eq userId) and (LectureProgress.lectureId eq lectureId)
        }.singleOrNull()

        if (existing != null) {
            LectureProgress.update({ (LectureProgress.userId eq userId) and (LectureProgress.lectureId eq lectureId) }) {
                it[progressIndex] = index
                it[selectedText] = quote
                it[updatedAt] = LocalDateTime.now()
            }
        } else {
            LectureProgress.insert {
                it[LectureProgress.userId] = userId
                it[LectureProgress.lectureId] = lectureId
                it[progressIndex] = index
                it[selectedText] = quote
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
        val sql = "SELECT u.email, COUNT(ta.test_id) as tests_count, COALESCE(AVG(ta.score), 0) as avg_score FROM users u JOIN test_attempts ta ON u.user_id = ta.user_id GROUP BY u.user_id, u.email ORDER BY (COUNT(ta.test_id) * AVG(ta.score)) DESC LIMIT 10;"
        val leaderboard = mutableListOf<LeaderboardItemDto>()
        val jdbcConnection = (connection.connection as Connection)
        val stmt = jdbcConnection.prepareStatement(sql)
        val rs = stmt.executeQuery()
        while (rs.next()) {
            leaderboard.add(LeaderboardItemDto(rs.getString("email"), String.format("%.1f", rs.getDouble("avg_score") * rs.getInt("tests_count")).replace(',', '.').toDouble(), rs.getInt("tests_count")))
        }
        stmt.close()
        leaderboard
    }

    override suspend fun getUserTestResults(userId: Int): List<Pair<Int, Int>> = dbQuery {
        val sql = "SELECT t.topic_id, ta.score FROM test_attempts ta JOIN tests t ON ta.test_id = t.test_id WHERE ta.user_id = ? ORDER BY ta.attempted_at ASC"
        val results = mutableListOf<Pair<Int, Int>>()
        execPattern(sql, listOf(userId)) { rs -> while (rs.next()) results.add(rs.getInt("topic_id") to rs.getInt("score")) }
        results
    }

    override suspend fun getFullProgress(userId: Int): ProgressDto = dbQuery {
        val globalSql = "WITH ordered_attempts AS (SELECT CAST(score AS FLOAT) as score_float, CAST(ROW_NUMBER() OVER (ORDER BY attempted_at) AS FLOAT) as rn FROM test_attempts WHERE user_id = ?) SELECT COUNT(*) as total_count, COALESCE(AVG(score_float), 0) as avg_score, COALESCE(REGR_SLOPE(score_float, rn), 0) as trend FROM ordered_attempts;"
        var totalTests = 0; var totalAvg = 0.0; var totalTrend = 0.0
        execPattern(globalSql, listOf(userId)) { rs -> if (rs.next()) { totalTests = rs.getInt("total_count"); totalAvg = rs.getDouble("avg_score"); totalTrend = rs.getDouble("trend") } }

        val disciplinesSql = "WITH ordered_attempts AS (SELECT d.name as discipline_name, d.discipline_id as discipline_id, CAST(ta.score AS FLOAT) as score_float, CAST(ROW_NUMBER() OVER (PARTITION BY d.discipline_id ORDER BY ta.attempted_at) AS FLOAT) as rn FROM test_attempts ta JOIN tests t ON ta.test_id = t.test_id JOIN topics top ON t.topic_id = top.topic_id JOIN disciplines d ON top.discipline_id = d.discipline_id WHERE ta.user_id = ?) SELECT discipline_id, discipline_name, COALESCE(AVG(score_float), 0) as avg_score, COALESCE(REGR_SLOPE(score_float, rn), 0) as trend FROM ordered_attempts GROUP BY discipline_id, discipline_name;"
        val disciplinesStats = mutableListOf<DisciplineStatDto>()
        execPattern(disciplinesSql, listOf(userId)) { rs -> while (rs.next()) disciplinesStats.add(DisciplineStatDto(rs.getInt("discipline_id"), rs.getString("discipline_name"), String.format("%.1f", rs.getDouble("avg_score")).replace(',', '.').toDouble(), String.format("%.2f", rs.getDouble("trend")).replace(',', '.').toDouble())) }

        val historySql = "SELECT score FROM test_attempts WHERE user_id = ? ORDER BY attempted_at ASC LIMIT 20"
        val history = mutableListOf<Int>()
        val stmtHistory = (connection.connection as Connection).prepareStatement(historySql)
        stmtHistory.setInt(1, userId)
        val rsHistory = stmtHistory.executeQuery()
        while (rsHistory.next()) history.add(rsHistory.getInt("score"))
        stmtHistory.close()

        ProgressDto(totalTests, String.format("%.1f", totalAvg).replace(',', '.').toDouble(), String.format("%.2f", totalTrend).replace(',', '.').toDouble(), disciplinesStats, history)
    }

    private fun <T> Transaction.execPattern(sql: String, params: List<Any>, transform: (ResultSet) -> T): T? {
        val jdbcConnection = (connection.connection as Connection)
        val stmt = jdbcConnection.prepareStatement(sql)
        params.forEachIndexed { index, value -> if (value is Int) stmt.setInt(index + 1, value) else if (value is String) stmt.setString(index + 1, value) }
        val rs = stmt.executeQuery(); val result = transform(rs); stmt.close(); return result
    }

    override suspend fun getAllDisciplines(): List<Discipline> = dbQuery { Disciplines.selectAll().map { Discipline(it[Disciplines.id], it[Disciplines.name], it[Disciplines.description]) } }
    override suspend fun getTopicsByDisciplineId(disciplineId: Int): List<Topic> = dbQuery { Topics.select { Topics.disciplineId eq disciplineId }.map { Topic(it[Topics.id], it[Topics.name], it[Topics.disciplineId]) } }

    override suspend fun getLectureByTopicId(topicId: Int): List<Lecture> = dbQuery {
        Lectures.select { Lectures.topicId eq topicId }
            .map { row ->
                val id = row[Lectures.id]

                // --- ИСПРАВЛЕНИЕ: Проверяем наличие теста для каждой лекции в списке ---
                val hasTest = Tests.select { Tests.lectureId eq id }.count() > 0

                Lecture(
                    id = id,
                    title = row[Lectures.title],
                    content = row[Lectures.content],
                    topicId = row[Lectures.topicId],
                    isFavorite = false, // Для списка избранное пока false (оптимизация)
                    hasTest = hasTest   // <--- ПЕРЕДАЕМ ПРАВИЛЬНОЕ ЗНАЧЕНИЕ
                )
            }
    }
    override suspend fun getLectureById(lectureId: Int, userId: Int): LectureDto? = dbQuery {
        val lectureRow = Lectures.select { Lectures.id eq lectureId }.singleOrNull() ?: return@dbQuery null
        val isFavorite = UserFavorites.select { (UserFavorites.lectureId eq lectureId) and (UserFavorites.userId eq userId) }.count() > 0
        // Проверяем наличие теста
        val hasTest = Tests.select { Tests.lectureId eq lectureId }.count() > 0
        LectureDto(lectureRow[Lectures.id], lectureRow[Lectures.title], lectureRow[Lectures.content], lectureRow[Lectures.topicId], isFavorite, hasTest)
    }
    override suspend fun addFavorite(userId: Int, lectureId: Int) = dbQuery { if (UserFavorites.select { (UserFavorites.userId eq userId) and (UserFavorites.lectureId eq lectureId) }.count() == 0L) UserFavorites.insert { it[UserFavorites.userId] = userId; it[UserFavorites.lectureId] = lectureId } }
    override suspend fun removeFavorite(userId: Int, lectureId: Int) = dbQuery { UserFavorites.deleteWhere { (UserFavorites.userId eq userId) and (UserFavorites.lectureId eq lectureId) }; Unit }
    override suspend fun getFavorites(userId: Int): List<Lecture> = dbQuery { (Lectures innerJoin UserFavorites).select { UserFavorites.userId eq userId }.map { Lecture(it[Lectures.id], it[Lectures.title], it[Lectures.content], it[Lectures.topicId], true) } }
    override suspend fun searchLectures(query: String): List<Lecture> = dbQuery { val q = "%${query.lowercase()}%"; Lectures.select { (Lectures.title.lowerCase() like q) or (Lectures.content.lowerCase() like q) }.map { Lecture(it[Lectures.id], it[Lectures.title], it[Lectures.content], it[Lectures.topicId]) } }
    override suspend fun saveTestAttempt(userId: Int, testId: Int, score: Int) = dbQuery { TestAttempts.insert { it[TestAttempts.userId] = userId; it[TestAttempts.testId] = testId; it[TestAttempts.score] = score }; Unit }
    override suspend fun getCorrectAnswers(testId: Int): Map<Int, List<Int>> = dbQuery {
        val result = mutableMapOf<Int, MutableList<Int>>()
        (Answers innerJoin Questions).slice(Answers.questionId, Answers.id).select { (Questions.testId eq testId) and (Answers.isCorrect eq true) }.forEach { result.computeIfAbsent(it[Answers.questionId]) { mutableListOf() }.add(it[Answers.id]) }
        result
    }
}