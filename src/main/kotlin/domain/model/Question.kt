package org.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val id: Int,
    val text: String,
    val answers: List<Answer> = emptyList()
)