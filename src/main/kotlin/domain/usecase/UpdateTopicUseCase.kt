package org.example.domain.usecase

import org.example.domain.repository.ContentRepository

class UpdateTopicUseCase(private val repository: ContentRepository) {
    suspend operator fun invoke(id: Int, name: String) {
        repository.updateTopic(id, name)
    }
}