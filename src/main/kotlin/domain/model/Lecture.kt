package org.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Lecture(
    val id: Int,
    val title: String,
    val content: String,
    val topicId: Int
)