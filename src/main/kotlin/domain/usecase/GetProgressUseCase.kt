package org.example.domain.usecase

import org.example.core.math.LinearRegression
import org.example.data.dto.ProgressDto
import org.example.domain.repository.ContentRepository

// Убрали @Inject constructor
class GetProgressUseCase(
    private val repository: ContentRepository
) {
    suspend operator fun invoke(userId: Int): ProgressDto {
        val results = repository.getUserTestResults(userId)

        if (results.isEmpty()) {
            return ProgressDto(0, 0.0, 0.0)
        }

        val scores = results.map { it.second }
        val totalTests = scores.size
        val avgScore = scores.average()

        // Математика
        val trend = LinearRegression.calculateTrend(scores)

        return ProgressDto(
            testsPassed = totalTests,
            averageScore = String.format("%.1f", avgScore).replace(',', '.').toDouble(),
            trend = String.format("%.2f", trend).replace(',', '.').toDouble()
        )
    }
}