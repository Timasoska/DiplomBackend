package org.example.domain.usecase

import org.example.data.dto.AdminTestResponse
import org.example.domain.repository.ContentRepository

class GetAdminTestUseCase(private val repository: ContentRepository) {

    /**
     * Получает тест для редактирования (с правильными ответами).
     * Можно передать либо topicId, либо lectureId.
     */
    suspend operator fun invoke(topicId: Int? = null, lectureId: Int? = null): AdminTestResponse? {
        return if (topicId != null) {
            repository.getFullTestByTopicId(topicId)
        } else if (lectureId != null) {
            repository.getFullTestByLectureId(lectureId)
        } else {
            null
        }
    }
}