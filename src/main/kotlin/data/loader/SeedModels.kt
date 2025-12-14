package org.example.data.loader

import kotlinx.serialization.Serializable

@Serializable
data class SeedDiscipline(
    val name: String,
    val description: String,
    val topics: List<SeedTopic>
)

@Serializable
data class SeedTopic(
    val name: String,
    val lectures: List<SeedLecture> = emptyList(),
    val test: SeedTest? = null // Теста может и не быть
)

@Serializable
data class SeedLecture(
    val title: String,
    val content: String
)

@Serializable
data class SeedTest(
    val title: String,
    val questions: List<SeedQuestion>
)

@Serializable
data class SeedQuestion(
    val text: String,
    val isMultipleChoice: Boolean = false, // <--- Добавили
    val difficulty: Int = 1, // <--- Добавили (по умолчанию 1)
    val answers: List<SeedAnswer>
)

@Serializable
data class SeedAnswer(
    val text: String,
    val isCorrect: Boolean
)