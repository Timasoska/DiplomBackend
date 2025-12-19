package org.example.domain.usecase

import org.example.data.dto.StudentRiskDto
import org.example.data.dto.TeacherGroupDto
import org.example.domain.repository.ContentRepository

class GroupUseCase(private val repository: ContentRepository) {

    suspend fun createGroup(teacherId: Int, disciplineId: Int, name: String) =
        repository.createGroup(teacherId, disciplineId, name)

    suspend fun joinGroup(studentId: Int, inviteCode: String) =
        repository.joinGroup(studentId, inviteCode)

    suspend fun getTeacherGroups(teacherId: Int): List<TeacherGroupDto> =
        repository.getTeacherGroups(teacherId)

    suspend fun getGroupAnalytics(groupId: Int): List<StudentRiskDto> =
        repository.getGroupRiskAnalytics(groupId)
}