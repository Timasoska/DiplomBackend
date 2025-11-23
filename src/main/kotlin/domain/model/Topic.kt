package org.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Topic(
    val id: Int,
    val name: String,
    val disciplineId: Int
)