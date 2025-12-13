package org.example.data.dto


import kotlinx.serialization.Serializable

@Serializable
data class LectureDto(
    val id: Int,
    val title: String,
    val content: String,
    val topicId: Int,
    val isFavorite: Boolean // <--- Добавили поле
)