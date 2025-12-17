package org.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Lecture(
    val id: Int,
    val title: String,
    val content: String,
    val topicId: Int,
    val isFavorite: Boolean = false, // Это поле у нас не сохраняется в БД для домена, но пусть будет для совместимости
    val hasTest: Boolean = false     // <--- ДОБАВЬ ЭТУ СТРОКУ
)