package org.example.domain.usecase

import org.example.data.dto.AdminTestResponse
import org.example.domain.repository.ContentRepository

class GetAdminTestUseCase(private val repository: ContentRepository) {
    suspend operator fun invoke(topicId: Int): AdminTestResponse? {
        return repository.getFullTestByTopicId(topicId)
    }
}