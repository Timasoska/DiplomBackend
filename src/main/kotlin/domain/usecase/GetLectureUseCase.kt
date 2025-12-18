package org.example.domain.usecase

import org.example.data.dto.LectureDto
import org.example.domain.model.Lecture
import org.example.domain.repository.ContentRepository

class GetLectureUseCase(private val repository: ContentRepository) {

    // Получить одну лекцию
    suspend fun byId(id: Int, userId: Int): LectureDto? {
        return repository.getLectureById(id, userId)
    }

    // Получить список лекций (ИЗМЕНЕНИЕ: добавили userId)
    suspend fun byTopicId(topicId: Int, userId: Int): List<Lecture> {
        return repository.getLectureByTopicId(topicId, userId)
    }
}