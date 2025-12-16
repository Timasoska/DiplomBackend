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
    val test: SeedTest? = null // Тест по теме
)

@Serializable
data class SeedLecture(
    val title: String,
    val content: String,
    val test: SeedTest? = null // <--- НОВОЕ: Тест по лекции
)

@Serializable
data class SeedTest(
    val title: String,
    val questions: List<SeedQuestion>,
    val timeLimit: Int = 0
)

@Serializable
data class SeedQuestion(
    val text: String,
    val isMultipleChoice: Boolean = false,
    val difficulty: Int = 1,
    val answers: List<SeedAnswer>
)

@Serializable
data class SeedAnswer(
    val text: String,
    val isCorrect: Boolean
)