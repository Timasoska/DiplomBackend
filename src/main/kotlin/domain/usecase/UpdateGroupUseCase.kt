package org.example.domain.usecase

import org.example.domain.repository.ContentRepository

class UpdateGroupUseCase(private val repository: ContentRepository) {
    suspend fun updateGroup(groupId: Int, name: String) {
        repository.updateGroup(groupId, name)
    }
}