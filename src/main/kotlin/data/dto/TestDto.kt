package org.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TestDto(
    val id: Int,
    val title: String,
    val questions: List<QuestionDto>,
    val timeLimit: Int = 0 // <--- НОВОЕ ПОЛЕ (секунды)
)