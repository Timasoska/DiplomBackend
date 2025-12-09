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

        val badTopicIds = resultsByTopic.filter { (topicId, scores) ->
            // АДАПТИВНАЯ ЛОГИКА v2.0:
            // Берем только последние 3 попытки
            val lastAttempts = scores.takeLast(3)

            // Если попыток хотя бы 3, и ВСЕ ПОСЛЕДНИЕ провальные (< 60)
            val isFallingBehind = lastAttempts.size >= 3 && lastAttempts.all { it < 60 }

            isFallingBehind
        }.keys

        val topicsToRepeat = mutableListOf<Topic>()
        val allDisciplines = repository.getAllDisciplines()

        allDisciplines.forEach { discipline ->
            val topics = repository.getTopicsByDisciplineId(discipline.id)
            topicsToRepeat.addAll(topics.filter { it.id in badTopicIds })
        }

        return topicsToRepeat
    }
}