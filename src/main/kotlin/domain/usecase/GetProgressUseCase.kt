package org.example.domain.usecase

import org.example.data.dto.ProgressDto
import org.example.domain.repository.ContentRepository

class GetProgressUseCase(
    private val repository: ContentRepository
) {
    suspend operator fun invoke(userId: Int): ProgressDto {
        // Вся математика ушла в SQL
        return repository.getFullProgress(userId)
    }
}