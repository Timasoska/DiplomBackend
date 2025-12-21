package org.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserGroupShortDto(val id: Int, val name: String) // Новая DTO

@Serializable
data class ProgressDto(
    val testsPassed: Int,
    val averageScore: Double,
    val trend: Double,
    val disciplines: List<DisciplineStatDto> = emptyList(),
    val history: List<Int> = emptyList(),
    val groups: List<UserGroupShortDto> = emptyList() // Теперь список объектов
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
    val email: String,
    val name: String, // <--- НОВОЕ ПОЛЕ
    val score: Double,
    val testsPassed: Int
)

@Serializable
data class TopicStatDto(
    val topicId: Int,
    val topicName: String,
    val averageScore: Double,
    val attemptsCount: Int,
    val lastScore: Int?
)

@Serializable
data class StudentDetailedReportDto(
    val email: String,
    val overallAverage: Double,
    val overallTrend: Double,
    val topics: List<TopicStatDto>,
    val attemptHistory: List<Int>
)