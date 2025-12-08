package org.example.data.repository

import org.example.data.db.* // Импорт всех таблиц
import org.example.domain.model.*
import org.example.domain.repository.ContentRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlin.text.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or

class ContentRepositoryImpl : ContentRepository {

    override suspend fun getUserTestResults(userId: Int): List<Pair<Int, Int>> = dbQuery {
        // Объединяем Попытки -> Тесты, чтобы узнать topicId
        (TestAttempts innerJoin Tests)
            .slice(Tests.topicId, TestAttempts.score)
            .select { TestAttempts.userId eq userId }
            .map { row ->
                row[Tests.topicId] to row[TestAttempts.score]
            }
    }

    override suspend fun getTestByTopicId(topicId: Int): Test? = dbQuery {
        // 1. Ищем сам тест
        val testRow = Tests.select { Tests.topicId eq topicId }.singleOrNull() ?: return@dbQuery null
        val testId = testRow[Tests.id]

        // 2. Ищем вопросы к тесту
        val questionsRows = Questions.select { Questions.testId eq testId }.toList()

        // 3. Собираем всё в структуру
        val questions = questionsRows.map { qRow ->
            val qId = qRow[Questions.id]
            // Для каждого вопроса ищем ответы
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
                answers = answers
            )
        }

        Test(
            id = testId,
            title = testRow[Tests.title],
            topicId = testRow[Tests.topicId],
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

    override suspend fun getCorrectAnswerId(questionId: Int): Int? = dbQuery {
        Answers.select { (Answers.questionId eq questionId) and (Answers.isCorrect eq true) }
            .map { it[Answers.id] }
            .singleOrNull()
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

    override suspend fun getLectureById(lectureId: Int): Lecture? = dbQuery {
        Lectures.select { Lectures.id eq lectureId }
            .map { row ->
                Lecture(
                    id = row[Lectures.id],
                    title = row[Lectures.title],
                    content = row[Lectures.content],
                    topicId = row[Lectures.topicId]
                )
            }
            .singleOrNull()
    }

    override suspend fun addFavorite(userId: Int, lectureId: Int) = dbQuery {
        // insertIgnore проигнорирует дубликаты (если уже добавлено)
        // Если insertIgnore не подсвечивается, используй просто insert, но оберни в try-catch
        UserFavorites.insert {
            it[UserFavorites.userId] = userId
            it[UserFavorites.lectureId] = lectureId
        }
        Unit // Возвращаем Unit (void)
    }

    override suspend fun removeFavorite(userId: Int, lectureId: Int) = dbQuery {
        UserFavorites.deleteWhere {
            (UserFavorites.userId eq userId) and (UserFavorites.lectureId eq lectureId)
        }
        Unit
    }

    override suspend fun getFavorites(userId: Int): List<Lecture> = dbQuery {
        // Магия SQL: Объединяем (JOIN) таблицу Избранного с Лекциями
        (Lectures innerJoin UserFavorites)
            .select { UserFavorites.userId eq userId }
            .map { row ->
                Lecture(
                    id = row[Lectures.id],
                    title = row[Lectures.title],
                    content = row[Lectures.content],
                    topicId = row[Lectures.topicId]
                )
            }
    }
}