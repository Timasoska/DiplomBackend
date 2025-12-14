package org.example.domain.usecase

import org.example.data.dto.TestResultDto
import org.example.domain.model.SubmitAnswerRequest
import org.example.domain.repository.ContentRepository

class SubmitTestUseCase(private val repository: ContentRepository) {

    suspend operator fun invoke(userId: Int, testId: Int, userAnswers: List<SubmitAnswerRequest>): TestResultDto {
        // 1. Получаем карту правильных ответов из БД: Map<QuestionId, List<CorrectAnswerId>>
        val correctMap = repository.getCorrectAnswers(testId)

        // 2. Группируем ответы пользователя: Map<QuestionId, List<SelectedAnswerId>>
        val userMap = userAnswers.groupBy({ it.questionId }, { it.answerId })

        var correctCount = 0
        val totalQuestions = correctMap.size // Считаем по количеству вопросов в тесте (из правильных ответов)

        // 3. Проверяем каждый вопрос
        correctMap.forEach { (qId, correctIds) ->
            val userSelectedIds = userMap[qId] ?: emptyList()

            // Логика: Ответ верен, если множества совпадают (порядок не важен)
            // Пользователь выбрал ВСЕ правильные и НЕ выбрал лишних
            if (userSelectedIds.toSet() == correctIds.toSet()) {
                correctCount++
            }
        }

        val score = if (totalQuestions > 0) {
            (correctCount.toDouble() / totalQuestions.toDouble() * 100).toInt()
        } else 0

        repository.saveTestAttempt(userId, testId, score)

        return TestResultDto(
            score = score,
            correctCount = correctCount,
            totalCount = totalQuestions,
            correctAnswers = correctMap // <--- Возвращаем ключи для подсветки на клиенте
        )
    }
}