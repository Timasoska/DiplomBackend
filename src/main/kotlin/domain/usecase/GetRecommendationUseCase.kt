package org.example.domain.usecase

import org.example.domain.model.Topic
import org.example.domain.repository.ContentRepository

class GetRecommendationsUseCase(private val repository: ContentRepository) {

    suspend operator fun invoke(userId: Int): List<Topic> {
        // 1. Получаем все результаты: List<Pair<TopicId, Score>>
        val results = repository.getUserTestResults(userId)

        // 2. Группируем результаты по ID темы
        // Map<TopicId, List<Score>>
        val resultsByTopic = results.groupBy(
            keySelector = { it.first },
            valueTransform = { it.second }
        )

        // 3. Применяем алгоритм фильтрации
        val badTopicIds = resultsByTopic.filter { (topicId, scores) ->
            // Условие из ТЗ:
            // "3 и более попытки" И "каждый раз менее 60%"
            val attemptsCount = scores.size
            val allFailed = scores.all { it < 60 }

            attemptsCount >= 3 && allFailed
        }.keys

        // 4. Получаем названия этих тем (нам нужны объекты Topic)
        // Для этого используем уже существующий метод поиска тем
        // (Немного неоптимально делать запросы в цикле, но для MVP отлично)
        val topicsToRepeat = mutableListOf<Topic>()
        val allDisciplines = repository.getAllDisciplines()

        // Пробегаем по всем дисциплинам и темам, чтобы найти нужные
        // (В реальном проекте лучше сделать метод getTopicById)
        allDisciplines.forEach { discipline ->
            val topics = repository.getTopicsByDisciplineId(discipline.id)
            topicsToRepeat.addAll(topics.filter { it.id in badTopicIds })
        }

        return topicsToRepeat
    }
}