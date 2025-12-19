package org.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TopicDto(
    val id: Int,
    val name: String,
    val disciplineId: Int,
    val progress: Int = 0 // <--- НОВОЕ ПОЛЕ (0-100%)
)