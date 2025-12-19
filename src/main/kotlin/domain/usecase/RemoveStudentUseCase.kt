package org.example.domain.usecase

import org.example.domain.repository.ContentRepository

class RemoveStudentUseCase(private val repository: ContentRepository) {
    suspend fun removeStudent(groupId: Int, studentId: Int) {
        repository.removeStudentFromGroup(groupId, studentId)
    }
}