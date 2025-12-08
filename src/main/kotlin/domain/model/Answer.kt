package org.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Answer(
    val id: Int,
    val text: String,
    val isCorrect: Boolean = false // Это поле мы будем скрывать при отправке на клиент
)

// DTO для отправки на клиент (без isCorrect)
@Serializable
data class AnswerDto(
    val id: Int,
    val text: String
)

// DTO для получения ответов от студента
@Serializable
data class SubmitAnswerRequest(
    val questionId: Int,
    val answerId: Int
)

@Serializable
data class TestResultResponse(
    val score: Int, // Процент
    val correctCount: Int,
    val totalCount: Int
)