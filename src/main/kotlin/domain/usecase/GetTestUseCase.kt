package org.example.domain.usecase

import org.example.domain.model.AnswerDto
import org.example.domain.model.Question
import org.example.domain.model.Test
import org.example.domain.repository.ContentRepository

class GetTestUseCase(private val repository: ContentRepository) {
    // Возвращаем Any, чтобы можно было подменить структуру ответов,
    // либо создадим специальный TestDto. Для простоты сейчас вернем Test, но с урезанными ответами вручную в Routing.
    // А лучше сделаем правильно сразу.

    suspend operator fun invoke(topicId: Int): Test? {
        return repository.getTestByTopicId(topicId)
    }
}