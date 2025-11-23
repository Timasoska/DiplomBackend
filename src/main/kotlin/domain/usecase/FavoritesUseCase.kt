package org.example.domain.usecase

import org.example.domain.model.Lecture
import org.example.domain.repository.ContentRepository

class FavoritesUseCase(private val repository: ContentRepository) {

    suspend fun add(userId: Int, lectureId: Int) {
        repository.addFavorite(userId, lectureId)
    }

    suspend fun remove(userId: Int, lectureId: Int) {
        repository.removeFavorite(userId, lectureId)
    }

    suspend fun getAll(userId: Int): List<Lecture> {
        return repository.getFavorites(userId)
    }
}