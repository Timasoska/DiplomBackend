package org.example.domain.usecase

import org.example.domain.repository.ContentRepository

class GetTeacherGroupsUseCase(private val repository: ContentRepository) {
    suspend fun getTeacherGroups(teacherId: Int) = repository.getTeacherGroups(teacherId)
}