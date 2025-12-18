package org.example.domain.usecase

import org.example.domain.repository.ContentRepository

class SaveTopicUseCase(private val repository: ContentRepository) {
    suspend operator fun invoke(disciplineId: Int, name: String) {
        repository.saveTopic(disciplineId, name)
    }
}