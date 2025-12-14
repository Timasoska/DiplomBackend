package org.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Test(
    val id: Int,
    val title: String,
    val topicId: Int,
    val timeLimit: Int = 0, // <--- НОВОЕ ПОЛЕ
    val questions: List<Question> = emptyList() // Вложенный список вопросов
)