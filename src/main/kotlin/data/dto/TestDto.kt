package org.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TestDto(
    val id: Int,
    val title: String,
    val timeLimit: Int = 0,
    val questions: List<QuestionDto>,
    val lectureId: Int? = null // <--- Добавим, чтобы клиент знал, чей это тест
)