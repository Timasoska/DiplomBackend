package org.example.domain.usecase

import org.example.domain.repository.ContentRepository

class DeleteTopicUseCase(private val repository: ContentRepository) {
    suspend operator fun invoke(id: Int) {
        repository.deleteTopic(id)
    }
}