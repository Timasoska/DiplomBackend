package org.example.domain.usecase

import org.example.domain.repository.ContentRepository

class DeleteLectureUseCase(private val repository: ContentRepository) {
    suspend operator fun invoke(id: Int) {
        repository.deleteLecture(id)
    }
}