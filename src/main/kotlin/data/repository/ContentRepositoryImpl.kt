package org.example.data.repository

import org.example.data.db.*
import org.example.data.db.Questions.testId
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
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

/**
 * Реализация репозитория данных (Backend).
 */
class ContentRepositoryImpl : ContentRepository {

    override suspend fun updateTopic(id: Int, name: String) = dbQuery {
        Topics.update({ Topics.id eq id }) {
            it[Topics.name] = name
        }
        Unit
    }

    override suspend fun deleteTopic(id: Int) = dbQuery {
        println("🗑️ [DELETE TOPIC] Starting deletion for Topic ID: $id")

        // 1. Получаем список ID лекций в этой теме
        val lectureIds = Lectures.select { Lectures.topicId eq id }.map { it[Lectures.id] }
        println("   -> Found ${lectureIds.size} lectures to delete")

        // 2. Получаем список ID тестов (Привязанных к теме ИЛИ к лекциям этой темы)
        val testIds = Tests.select {
            (Tests.topicId eq id) or (Tests.lectureId inList lectureIds)
        }.map { it[Tests.id] }
        println("   -> Found ${testIds.size} tests to delete")

        // 3. Удаляем ТЕСТЫ и их внутренности
        if (testIds.isNotEmpty()) {
            // Статистика попыток
            TestAttempts.deleteWhere { testId inList testIds }

            // Вопросы и Ответы
            val questionIds = Questions.select { testId inList testIds }.map { it[Questions.id] }
            if (questionIds.isNotEmpty()) {
                Answers.deleteWhere { questionId inList questionIds }
            }
            Questions.deleteWhere { testId inList testIds }

            // Сами тесты
            Tests.deleteWhere { Tests.id inList testIds }
            println("   -> Tests data cleared")
        }

        // 4. Удаляем ЛЕКЦИИ и их связи
        if (lectureIds.isNotEmpty()) {
            UserFavorites.deleteWhere { lectureId inList lectureIds }
            LectureProgress.deleteWhere { lectureId inList lectureIds }
            LectureFiles.deleteWhere { lectureId inList lectureIds } // Если добавляли эту таблицу

            Lectures.deleteWhere { topicId eq id }
            println("   -> Lectures data cleared")
        }

        // 5. Удаляем саму ТЕМУ
        Topics.deleteWhere { Topics.id eq id }
        println("✅ [DELETE TOPIC] Topic $id deleted successfully")
        Unit
    }

    override suspend fun saveTopic(disciplineId: Int, name: String) = dbQuery {
        Topics.insert {
            it[Topics.name] = name
            it[Topics.disciplineId] = disciplineId
        }
        Unit
    }

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
    override suspend fun getTopicsByDisciplineId(disciplineId: Int, userId: Int): List<Topic> = dbQuery {
        Topics.select { Topics.disciplineId eq disciplineId }
            .map { row ->
                val topicId = row[Topics.id]

                // --- НОВАЯ ЛОГИКА: Оценка за тест по теме ---

                // 1. Ищем тест, привязанный к этой теме (lectureId is NULL)
                val testRow = Tests
                    .select { (Tests.topicId eq topicId) and (Tests.lectureId.isNull()) }
                    .singleOrNull()

                var topicScore: Int? = null

                if (testRow != null) {
                    val testId = testRow[Tests.id]

                    val lastAttempt = TestAttempts
                        .select { (TestAttempts.testId eq testId) and (TestAttempts.userId eq userId) }
                        .orderBy(TestAttempts.attemptedAt to SortOrder.DESC)
                        .limit(1)
                        .singleOrNull()

                    // ИЗМЕНЕНИЕ: Убрали '?: 0'. Если попытки нет, будет null.
                    topicScore = lastAttempt?.get(TestAttempts.score)
                }

                Topic(
                    id = topicId,
                    name = row[Topics.name],
                    disciplineId = row[Topics.disciplineId],
                    progress = topicScore // <--- Передаем null или число
                )
            }
    }
    override suspend fun getLectureByTopicId(topicId: Int, userId: Int): List<Lecture> = dbQuery {
        Lectures.select { Lectures.topicId eq topicId }
            .map { row ->
                val id = row[Lectures.id]

                // Проверяем тест
                val testRow = Tests.select { Tests.lectureId eq id }.singleOrNull()
                val hasTest = testRow != null

                var userScore: Int? = null
                if (hasTest && userId != 0) {
                    val testId = testRow!![Tests.id]

                    // ИСПРАВЛЕНИЕ: Берем ПОСЛЕДНЮЮ попытку, а не максимальную
                    // Сортируем по дате убывания и берем первую запись
                    val lastAttempt = TestAttempts
                        .select { (TestAttempts.testId eq testId) and (TestAttempts.userId eq userId) }
                        .orderBy(TestAttempts.attemptedAt to SortOrder.DESC)
                        .limit(1)
                        .singleOrNull()

                    userScore = lastAttempt?.get(TestAttempts.score)
                }

                Lecture(
                    id = id,
                    title = row[Lectures.title],
                    content = row[Lectures.content],
                    topicId = row[Lectures.topicId],
                    isFavorite = false,
                    hasTest = hasTest,
                    userScore = userScore // <--- Записываем балл
                    // files пока не грузим в списке для скорости
                )
            }
    }

    override suspend fun getLectureById(lectureId: Int, userId: Int): LectureDto? = dbQuery {
        val lectureRow = Lectures.select { Lectures.id eq lectureId }.singleOrNull()
            ?: return@dbQuery null

        val isFavorite = UserFavorites.select {
            (UserFavorites.lectureId eq lectureId) and (UserFavorites.userId eq userId)
        }.count() > 0

        val testRow = Tests.select { Tests.lectureId eq lectureId }.singleOrNull()
        val hasTest = testRow != null

        var userScore: Int? = null
        if (hasTest && userId != 0) {
            val testId = testRow!![Tests.id]

            // ИСПРАВЛЕНИЕ: Берем ПОСЛЕДНЮЮ попытку, а не максимальную
            // Сортируем по дате убывания и берем первую запись
            val lastAttempt = TestAttempts
                .select { (TestAttempts.testId eq testId) and (TestAttempts.userId eq userId) }
                .orderBy(TestAttempts.attemptedAt to SortOrder.DESC)
                .limit(1)
                .singleOrNull()

            userScore = lastAttempt?.get(TestAttempts.score)
        }

        // Файлы
        val files = LectureFiles.select { LectureFiles.lectureId eq lectureId }.map {
            LectureFileDto(
                id = it[LectureFiles.id],
                title = it[LectureFiles.title],
                url = "/api/files/${it[LectureFiles.id]}" // Ссылка на скачивание
            )
        }

        LectureDto(
            id = lectureRow[Lectures.id],
            title = lectureRow[Lectures.title],
            content = lectureRow[Lectures.content],
            topicId = lectureRow[Lectures.topicId],
            isFavorite = isFavorite,
            hasTest = hasTest,
            userScore = userScore, // <--- Новый
            files = files          // <--- Новое
        )
    }

    // 4. Прикрепление файла
    override suspend fun attachFileToLecture(lectureId: Int, title: String, filePath: String) = dbQuery {
        LectureFiles.insert {
            it[LectureFiles.lectureId] = lectureId
            it[LectureFiles.title] = title
            it[LectureFiles.filePath] = filePath
        }
        Unit
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

    override suspend fun createGroup(teacherId: Int, disciplineId: Int, name: String): String = dbQuery {
        // Генерируем простой уникальный код (например, 6 символов)
        val code = UUID.randomUUID().toString().substring(0, 6).uppercase()

        StudentGroups.insert {
            it[StudentGroups.teacherId] = teacherId
            it[StudentGroups.disciplineId] = disciplineId
            it[StudentGroups.name] = name
            it[StudentGroups.inviteCode] = code
        }
        code
    }

    override suspend fun joinGroup(studentId: Int, inviteCode: String): Result<Unit> = dbQuery {
        val group = StudentGroups.select { StudentGroups.inviteCode eq inviteCode }.singleOrNull()
            ?: return@dbQuery Result.failure(Exception("Group not found"))

        val groupId = group[StudentGroups.id]

        // Проверяем, не вступил ли уже
        val exists = GroupMembers.select {
            (GroupMembers.userId eq studentId) and (GroupMembers.groupId eq groupId)
        }.count() > 0

        if (!exists) {
            GroupMembers.insert {
                it[GroupMembers.userId] = studentId
                it[GroupMembers.groupId] = groupId
            }
        }
        Result.success(Unit)
    }

    override suspend fun getTeacherGroups(teacherId: Int): List<TeacherGroupDto> = dbQuery {
        (StudentGroups innerJoin Disciplines)
            .select { StudentGroups.teacherId eq teacherId }
            .map { row ->
                val groupId = row[StudentGroups.id]
                val count = GroupMembers.select { GroupMembers.groupId eq groupId }.count().toInt()

                TeacherGroupDto(
                    id = groupId,
                    name = row[StudentGroups.name],
                    disciplineName = row[Disciplines.name],
                    inviteCode = row[StudentGroups.inviteCode],
                    studentCount = count
                )
            }
    }

    /**
     * Реализация Risk Clustering (Кластеризация рисков).
     * 1. Находит всех студентов группы.
     * 2. Фильтрует оценки ТОЛЬКО по предмету этой группы.
     * 3. Считает Среднее и Тренд (Линейная регрессия).
     * 4. Присваивает статус (Green/Yellow/Red).
     */
    override suspend fun getGroupRiskAnalytics(groupId: Int): List<StudentRiskDto> = dbQuery {
        // 1. Узнаем дисциплину группы
        val disciplineId = StudentGroups.slice(StudentGroups.disciplineId)
            .select { StudentGroups.id eq groupId }
            .singleOrNull()
            ?.get(StudentGroups.disciplineId) ?: return@dbQuery emptyList()

        // 2. Получаем список студентов
        val students = (Users innerJoin GroupMembers)
            .slice(Users.id, Users.email)
            .select { GroupMembers.groupId eq groupId }
            .map { it[Users.id] to it[Users.email] }

        val result = mutableListOf<StudentRiskDto>()

        for ((studentId, email) in students) {
            // 3. Выбираем попытки тестов ТОЛЬКО по этой дисциплине
            // Join: TestAttempts -> Tests -> Topics (где disciplineId совпадает)
            // Учитываем и тесты лекций, и тесты тем

            // Сложный запрос через API Exposed или через Raw SQL.
            // Для надежности используем логику фильтрации в коде (так как данных не миллионы).

            // Получаем все ID тестов, относящихся к этой дисциплине
            val topicIds = Topics.select { Topics.disciplineId eq disciplineId }.map { it[Topics.id] }

            // Ищем тесты, привязанные к этим топикам, ИЛИ к лекциям этих топиков
            // (Упрощение: считаем все попытки этого юзера, фильтруем по topicId)

            // Для диплома: SQL запрос для получения оценок студента по конкретной дисциплине
            val sql = """
                SELECT ta.score, ta.attempted_at
                FROM test_attempts ta
                JOIN tests t ON ta.test_id = t.test_id
                LEFT JOIN topics top ON t.topic_id = top.topic_id
                LEFT JOIN lectures l ON t.lecture_id = l.lecture_id
                LEFT JOIN topics top_l ON l.topic_id = top_l.topic_id
                WHERE ta.user_id = ? 
                AND (top.discipline_id = ? OR top_l.discipline_id = ?)
                ORDER BY ta.attempted_at ASC
            """.trimIndent()

            val scores = mutableListOf<Int>()

            val stmt = (connection.connection as java.sql.Connection).prepareStatement(sql)
            stmt.setInt(1, studentId)
            stmt.setInt(2, disciplineId)
            stmt.setInt(3, disciplineId)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                scores.add(rs.getInt("score"))
            }
            stmt.close()

            // 4. Математика
            val average = if (scores.isNotEmpty()) scores.average() else 0.0
            val trend = calculateTrendSimple(scores) // Локальная функция расчета

            // 5. Кластеризация (Risk Logic)
            val risk = when {
                average >= 80 && trend >= -0.5 -> RiskLevel.GREEN
                average < 50 || trend < -2.0 -> RiskLevel.RED // Двойка или резкое падение
                else -> RiskLevel.YELLOW
            }

            result.add(StudentRiskDto(studentId, email, average, trend, risk))
        }

        // Сортируем: сначала Красные (проблемные), потом Желтые, потом Зеленые
        result.sortedByDescending { it.riskLevel } // RED > YELLOW > GREEN (по enum ordinal)
    }

    // Вспомогательная функция МНК (Метод Наименьших Квадратов) для списка чисел
    private fun calculateTrendSimple(scores: List<Int>): Double {
        if (scores.size < 2) return 0.0
        val n = scores.size.toDouble()
        var sumX = 0.0
        var sumY = 0.0
        var sumXY = 0.0
        var sumX2 = 0.0

        scores.forEachIndexed { index, score ->
            val x = index.toDouble()
            val y = score.toDouble()
            sumX += x; sumY += y; sumXY += x * y; sumX2 += x * x
        }
        val denominator = n * sumX2 - sumX * sumX
        return if (denominator == 0.0) 0.0 else (n * sumXY - sumX * sumY) / denominator
    }

}