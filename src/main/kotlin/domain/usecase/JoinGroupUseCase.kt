package org.example.domain.usecase

import org.example.domain.repository.ContentRepository

class JoinGroupUseCase(private val repository: ContentRepository) {
    suspend fun joinGroup(studentId: Int, inviteCode: String) =
        repository.joinGroup(studentId, inviteCode)
}