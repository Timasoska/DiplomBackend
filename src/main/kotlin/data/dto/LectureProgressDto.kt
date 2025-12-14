package org.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LectureProgressDto(
    val lectureId: Int,
    val progressIndex: Int, // Индекс для скролла
    val quote: String? = null // Значение по умолчанию
)

@Serializable
data class UpdateProgressRequest(
    val progressIndex: Int,
    val quote: String? = null // <--- ТЕПЕРЬ МОЖНО НЕ ОТПРАВЛЯТЬ ЭТО ПОЛЕ В JSON
)