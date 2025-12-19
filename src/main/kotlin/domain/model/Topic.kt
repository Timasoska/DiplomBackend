package org.example.domain.model

import kotlinx.serialization.Serializable

@Serializable // <--- ВАЖНО
data class Topic(
    val id: Int,
    val name: String,
    val disciplineId: Int,
    val progress: Int = 0
)