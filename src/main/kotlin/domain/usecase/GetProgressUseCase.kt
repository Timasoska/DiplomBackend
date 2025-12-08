package org.example.domain.usecase

import kotlinx.serialization.Serializable
import org.example.domain.repository.ContentRepository

@Serializable
data class ProgressResponse(
    val testsPassed: Int,
    val averageScore: Double
)

class GetProgressUseCase(private val repository: ContentRepository) {
    suspend operator fun invoke(userId: Int): ProgressResponse {
        val results = repository.getUserTestResults(userId)

        if (results.isEmpty()) {
            return ProgressResponse(0, 0.0)
        }

        val totalTests = results.size
        // Считаем среднее арифметическое всех баллов
        val avgScore = results.map { it.second }.average()

        return ProgressResponse(
            testsPassed = totalTests,
            averageScore = String.format("%.1f", avgScore).replace(',', '.').toDouble()
            // Округляем до 1 знака
        )
    }
}