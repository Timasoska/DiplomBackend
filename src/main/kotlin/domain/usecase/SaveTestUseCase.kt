package org.example.domain.usecase

import org.example.data.dto.SaveTestRequest
import org.example.domain.repository.ContentRepository

class SaveTestUseCase(private val repository: ContentRepository) {
    suspend operator fun invoke(request: SaveTestRequest) {
        repository.saveTest(request)
    }
}