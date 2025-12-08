package org.example.domain.usecase

import org.example.domain.model.Lecture
import org.example.domain.repository.ContentRepository

class SearchUseCase(private val repository: ContentRepository) {
    suspend operator fun invoke(query: String): List<Lecture> {
        if (query.isBlank()) return emptyList()
        return repository.searchLectures(query)
    }
}