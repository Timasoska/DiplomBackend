package org.example.domain.usecase

import org.example.domain.model.SubmitAnswerRequest
import org.example.domain.model.TestResultResponse
import org.example.domain.repository.ContentRepository

class SubmitTestUseCase(private val repository: ContentRepository) {

    suspend operator fun invoke(userId: Int, testId: Int, userAnswers: List<SubmitAnswerRequest>): TestResultResponse {
        var correctCount = 0

        userAnswers.forEach { userAnswer ->
            val correctId = repository.getCorrectAnswerId(userAnswer.questionId)
            if (correctId == userAnswer.answerId) {
                correctCount++
            }
        }

        val totalQuestions = userAnswers.size
        // В идеале totalQuestions нужно брать из базы, но для MVP возьмем по количеству ответов

        val score = if (totalQuestions > 0) {
            (correctCount.toDouble() / totalQuestions.toDouble() * 100).toInt()
        } else 0

        // Сохраняем результат в историю (это нужно для Адаптивного обучения потом!)
        repository.saveTestAttempt(userId, testId, score)

        return TestResultResponse(
            score = score,
            correctCount = correctCount,
            totalCount = totalQuestions
        )
    }
}