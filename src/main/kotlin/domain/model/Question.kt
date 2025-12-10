package org.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val id: Int,
    val text: String,
    val difficulty: Int, // <--- ДОБАВЬ ЭТУ СТРОКУ
    val answers: List<Answer> = emptyList()
)