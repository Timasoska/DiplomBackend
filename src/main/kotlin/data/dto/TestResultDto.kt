package org.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TestResultDto(
    val score: Int,
    val correctCount: Int,
    val totalCount: Int,
    // ID вопроса -> Список ID правильных ответов (для подсветки на клиенте)
    val correctAnswers: Map<Int, List<Int>> = emptyMap() // <--- Добавили
)