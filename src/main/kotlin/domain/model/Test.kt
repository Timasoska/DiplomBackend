package org.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Test(
    val id: Int,
    val title: String,
    val topicId: Int?,    // <--- Nullable
    val lectureId: Int?,  // <--- Nullable
    val timeLimit: Int = 0,
    val questions: List<Question> = emptyList()
)