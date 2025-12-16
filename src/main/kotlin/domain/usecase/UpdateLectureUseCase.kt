package org.example.domain.usecase

import org.example.domain.repository.ContentRepository

class UpdateLectureUseCase(private val repository: ContentRepository) {
    suspend operator fun invoke(id: Int, title: String, content: String) {
        repository.updateLecture(id, title, content)
    }
}