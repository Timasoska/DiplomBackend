package org.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProgressDto(
    val testsPassed: Int,
    val averageScore: Double,
    val trend: Double // <--- Новое поле для математики
)