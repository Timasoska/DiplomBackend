package org.example.domain.usecase

import org.example.data.dto.LectureProgressDto
import org.example.domain.repository.ContentRepository

class LectureProgressUseCase(private val repository: ContentRepository) {

    suspend fun saveProgress(userId: Int, lectureId: Int, index: Int, quote: String?) {
        repository.saveLectureProgress(userId, lectureId, index, quote)
    }

    suspend fun getProgress(userId: Int, lectureId: Int): LectureProgressDto? {
        return repository.getLectureProgress(userId, lectureId)
    }
}