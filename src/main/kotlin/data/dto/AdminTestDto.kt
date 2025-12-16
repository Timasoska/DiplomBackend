package org.example.data.dto
import kotlinx.serialization.Serializable

@Serializable
data class SaveTestRequest(
    val topicId: Int? = null,   // Стало nullable
    val lectureId: Int? = null, // Новое поле
    val title: String,
    val timeLimit: Int,
    val questions: List<SaveQuestionRequest>
)

@Serializable
data class SaveQuestionRequest(
    val text: String,
    val difficulty: Int,
    val isMultipleChoice: Boolean,
    val answers: List<SaveAnswerRequest>
)

@Serializable
data class SaveAnswerRequest(
    val text: String,
    val isCorrect: Boolean
)

@Serializable
data class AdminTestResponse(
    val id: Int,
    val topicId: Int?,   // Стало nullable
    val lectureId: Int?, // Новое поле
    val title: String,
    val timeLimit: Int,
    val questions: List<SaveQuestionRequest>
)