package org.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProgressDto(
    val testsPassed: Int,
    val averageScore: Double,
    val trend: Double,
    val disciplines: List<DisciplineStatDto> = emptyList() // <--- Новый список
)

@Serializable
data class DisciplineStatDto(
    val id: Int,
    val name: String,
    val averageScore: Double,
    val trend: Double
)

@Serializable
data class LeaderboardItemDto(
    val email: String, // Имя пользователя (пока у нас только email)
    val score: Double, // Рейтинг
    val testsPassed: Int // Сколько прошел
)