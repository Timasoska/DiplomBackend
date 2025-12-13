package org.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Lecture(
    val id: Int,
    val title: String,
    val content: String,
    val topicId: Int,
    val isFavorite: Boolean = false // <--- ДОБАВЬ ЭТУ СТРОКУ (с дефолтным значением false)
)