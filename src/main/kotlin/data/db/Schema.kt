package org.example.data.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

// 1. Пользователи
object Users : Table("users") {
    val id = integer("user_id").autoIncrement()
    val email = varchar("email", 128).uniqueIndex()
    val passwordHash = varchar("password_hash", 256)
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }

    override val primaryKey = PrimaryKey(id)
}

// 2. Дисциплины (Уголовное право и т.д.)
object Disciplines : Table("disciplines") {
    val id = integer("discipline_id").autoIncrement()
    val name = varchar("name", 255)
    val description = text("description")

    override val primaryKey = PrimaryKey(id)
}

// 3. Темы
object Topics : Table("topics") {
    val id = integer("topic_id").autoIncrement()
    val name = varchar("name", 255)
    // Связь Many-to-One с Disciplines
    val disciplineId = integer("discipline_id").references(Disciplines.id)

    override val primaryKey = PrimaryKey(id)
}

// 4. Лекции
object Lectures : Table("lectures") {
    val id = integer("lecture_id").autoIncrement()
    val title = varchar("title", 255)
    val content = text("content")
    val topicId = integer("topic_id").references(Topics.id)

    override val primaryKey = PrimaryKey(id)
}

// 5. Тесты
object Tests : Table("tests") {
    val id = integer("test_id").autoIncrement()
    val title = varchar("title", 255)
    val topicId = integer("topic_id").references(Topics.id)

    override val primaryKey = PrimaryKey(id)
}

// 6. Вопросы
object Questions : Table("questions") {
    val id = integer("question_id").autoIncrement()
    val questionText = text("question_text")
    val testId = integer("test_id").references(Tests.id)

    /**
     * Сложность вопроса:
     * 1 - Легкий (Зеленый)
     * 2 - Средний (Желтый)
     * 3 - Сложный (Красный)
     */
    val difficulty = integer("difficulty").default(1) // <--- Добавили колонку

    override val primaryKey = PrimaryKey(id)
}

// 7. Варианты ответов
object Answers : Table("answers") {
    val id = integer("answer_id").autoIncrement()
    val answerText = text("answer_text")
    val isCorrect = bool("is_correct")
    val questionId = integer("question_id").references(Questions.id)

    override val primaryKey = PrimaryKey(id)
}

// 8. Попытки прохождения тестов (для Аналитики и Адаптивного обучения)
object TestAttempts : Table("test_attempts") {
    val id = integer("attempt_id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val testId = integer("test_id").references(Tests.id)
    val score = integer("score") // В процентах (0-100)
    val attemptedAt = datetime("attempted_at").clientDefault { LocalDateTime.now() }

    override val primaryKey = PrimaryKey(id)
}

// 9. Избранное (UserFavorites)
object UserFavorites : Table("user_favorites") {
    val id = integer("favorite_id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val lectureId = integer("lecture_id").references(Lectures.id)
    // Если null - сохранена вся лекция, иначе конкретный фрагмент
    val selectedText = text("selected_text").nullable()
    val addedAt = datetime("added_at").clientDefault { LocalDateTime.now() }

    override val primaryKey = PrimaryKey(id)
}

