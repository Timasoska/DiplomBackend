package org.example.domain.usecase

import org.example.data.dto.LectureDto
import org.example.domain.model.Lecture
import org.example.domain.repository.ContentRepository

class GetLectureUseCase(private val repository: ContentRepository) {
    // Получить лекцию по ID (для чтения)
    // 1. Добавляем userId
    // 2. Меняем возвращаемый тип на LectureDto? (чтобы передать isFavorite)
    suspend fun byId(id: Int, userId: Int): LectureDto? {
        return repository.getLectureById(id, userId)
    }

    // Получить список лекций по теме (для списка)
    suspend fun byTopicId(topicId: Int): List<Lecture> {
        return repository.getLectureByTopicId(topicId)
    }
}