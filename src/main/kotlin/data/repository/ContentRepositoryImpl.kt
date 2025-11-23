package org.example.data.repository

import org.example.data.db.* // Импорт всех таблиц
import org.example.domain.model.Discipline
import org.example.domain.model.Lecture
import org.example.domain.model.Topic
import org.example.domain.repository.ContentRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlin.text.insert

class ContentRepositoryImpl : ContentRepository {

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