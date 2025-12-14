package org.example.domain.usecase

import org.example.domain.model.AnswerDto
import org.example.domain.model.Question
import org.example.domain.model.Test
import org.example.domain.repository.ContentRepository

/**
 * Получение теста.
 * Реализует логику "Анти-списывание": вопросы и ответы перемешиваются на сервере.
 */
class GetTestUseCase(private val repository: ContentRepository) {

    suspend operator fun invoke(topicId: Int): Test? {
        val test = repository.getTestByTopicId(topicId) ?: return null

        // 1. Перемешиваем вопросы
        val shuffledQuestions = test.questions.shuffled().map { question ->
            // 2. Перемешиваем варианты ответов внутри вопроса
            question.copy(answers = question.answers.shuffled())
        }

        return test.copy(questions = shuffledQuestions)
    }
}