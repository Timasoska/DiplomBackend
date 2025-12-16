package org.example.domain.usecase

import org.example.domain.model.Test
import org.example.domain.repository.ContentRepository

class GetTestByLectureUseCase(private val repository: ContentRepository) {

    suspend operator fun invoke(lectureId: Int): Test? {
        // 1. Получаем тест из репозитория
        val test = repository.getTestByLectureId(lectureId) ?: return null

        // 2. Внедряем логику "Анти-списывание" (как и в GetTestUseCase)
        val shuffledQuestions = test.questions.shuffled().map { question ->
            // Перемешиваем варианты ответов внутри вопроса
            question.copy(answers = question.answers.shuffled())
        }

        return test.copy(questions = shuffledQuestions)
    }
}