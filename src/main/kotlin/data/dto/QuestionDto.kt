package org.example.data.dto

import kotlinx.serialization.Serializable
import org.example.domain.model.AnswerDto

@Serializable
data class QuestionDto(
    val id: Int,
    val text: String,
    val difficulty: Int,
    val isMultipleChoice: Boolean, // <--- Добавили
    val answers: List<AnswerDto>
)