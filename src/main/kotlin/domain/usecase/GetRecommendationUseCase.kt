package org.example.domain.usecase

import org.example.domain.model.Topic
import org.example.domain.repository.ContentRepository

class GetRecommendationsUseCase(private val repository: ContentRepository) {

    suspend operator fun invoke(userId: Int): List<Topic> {
        val results = repository.getUserTestResults(userId)

        val resultsByTopic = results.groupBy(
            keySelector = { it.first },
            valueTransform = { it.second }
        )

        val badTopicIds = resultsByTopic.filter { (topicId, attempts) ->
            val lastAttempts = attempts.takeLast(3)

            // Математический подход: вычисляем среднее последних попыток
            val averageRecent = if (lastAttempts.isNotEmpty()) lastAttempts.average() else 100.0

            // Условие 1: Средний балл за последнее время ниже 60%
            val lowAverage = lastAttempts.size >= 2 && averageRecent < 60.0

            // Условие 2: Резкий спад (например, последняя оценка намного хуже предпоследней)
            val sharpDrop = lastAttempts.size >= 2 && (lastAttempts[lastAttempts.size - 2] - lastAttempts.last()) > 40

            lowAverage || sharpDrop
        }.keys

        val topicsToRepeat = mutableListOf<Topic>()
        val allDisciplines = repository.getAllDisciplines()

        allDisciplines.forEach { discipline ->
            // ИСПРАВЛЕНИЕ: Добавили userId вторым аргументом
            val topics = repository.getTopicsByDisciplineId(discipline.id, userId)

            topicsToRepeat.addAll(topics.filter { it.id in badTopicIds })
        }

        return topicsToRepeat
    }
}