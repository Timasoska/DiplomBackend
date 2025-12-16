package org.example.data.dto
import kotlinx.serialization.Serializable

@Serializable
data class SaveTestRequest(
    val topicId: Int,
    val title: String,
    val timeLimit: Int, // В секундах
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

// --- НОВЫЙ DTO ДЛЯ ОТВЕТА ---
@Serializable
data class AdminTestResponse(
    val id: Int,
    val topicId: Int,
    val title: String,
    val timeLimit: Int,
    val questions: List<SaveQuestionRequest> // Переиспользуем структуру вопроса
)