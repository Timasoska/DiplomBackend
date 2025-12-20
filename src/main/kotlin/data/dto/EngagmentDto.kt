package org.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class EngagementStatusDto(
    val streak: Int,
    val todayXp: Int,
    val dailyGoalXp: Int = 100, // Цель фиксированная или динамическая
    val totalXp: Int,
    val isDailyGoalReached: Boolean
)

@Serializable
data class AddXpRequest(
    val amount: Int,
    val source: String // "lecture", "test", "flashcard"
)