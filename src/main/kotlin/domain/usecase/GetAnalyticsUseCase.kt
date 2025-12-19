package org.example.domain.usecase

import org.example.data.dto.StudentRiskDto
import org.example.domain.repository.ContentRepository

/**
 * Сценарий получения аналитики рисков для группы студентов.
 */
class GetAnalyticsUseCase(private val repository: ContentRepository) {
    suspend fun getAnalytics(groupId: Int): List<StudentRiskDto> {
        return repository.getGroupRiskAnalytics(groupId)
    }
}