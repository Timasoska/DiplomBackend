package org.example.data.dto


import kotlinx.serialization.Serializable

@Serializable
data class LectureDto(
    val id: Int,
    val title: String,
    val content: String,
    val topicId: Int,
    val isFavorite: Boolean,
    val hasTest: Boolean = false,
    val userScore: Int? = null, // null = не проходил, 0-100 = результат
    val files: List<LectureFileDto> = emptyList()
)

@Serializable
data class LectureFileDto(
    val id: Int,
    val title: String,
    val url: String // Ссылка для скачивания
)