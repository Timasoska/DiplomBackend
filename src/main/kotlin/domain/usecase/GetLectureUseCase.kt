package org.example.domain.usecase

import org.example.domain.model.Lecture
import org.example.domain.repository.ContentRepository

class GetLectureUseCase(private val repository: ContentRepository) {
    // Получить лекцию по ID (для чтения)
    suspend fun byId(id: Int): Lecture? {
        return repository.getLectureById(id)
    }

    // Получить список лекций по теме (для списка)
    suspend fun byTopicId(topicId: Int): List<Lecture> {
        return repository.getLectureByTopicId(topicId)
    }
}