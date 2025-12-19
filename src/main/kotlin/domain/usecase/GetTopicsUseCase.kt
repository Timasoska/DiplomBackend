package org.example.domain.usecase

import org.example.domain.model.Topic
import org.example.domain.repository.ContentRepository

class GetTopicsUseCase(private val repository: ContentRepository) {
    suspend operator fun invoke(disciplineId: Int, userId: Int): List<Topic> {
        return repository.getTopicsByDisciplineId(disciplineId, userId)
    }
}