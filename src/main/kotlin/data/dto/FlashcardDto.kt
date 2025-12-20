package org.example.data.dto

import kotlinx.serialization.Serializable

/**
 * Карточка для повторения (вопрос + варианты).
 */
@Serializable
data class FlashcardDto(
    val questionId: Int,
    val text: String,
    val options: List<FlashcardOptionDto> // Варианты ответов
)

@Serializable
data class FlashcardOptionDto(
    val id: Int,
    val text: String
)

/**
 * Запрос на сохранение результата повторения.
 */
@Serializable
data class ReviewFlashcardRequest(
    val questionId: Int,
    // Оценка качества: 0 (Не знаю), 3 (Вспомнил с трудом), 5 (Легко)
    val quality: Int
)