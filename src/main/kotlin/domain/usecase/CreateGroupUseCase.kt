package org.example.domain.usecase

import org.example.domain.repository.ContentRepository

class CreateGroupUseCase(private val repository: ContentRepository) {
    suspend fun createGroup(teacherId: Int, disciplineId: Int, name: String) =
        repository.createGroup(teacherId, disciplineId, name)
}