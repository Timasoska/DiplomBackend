package org.example.domain.usecase

import org.example.domain.repository.ContentRepository

class DeleteGroupUseCase(private val repository: ContentRepository) {
    suspend fun deleteGroup(groupId: Int) {
        repository.deleteGroup(groupId)
    }
}