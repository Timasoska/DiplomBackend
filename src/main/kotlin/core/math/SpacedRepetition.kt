package org.example.core.math

import java.time.LocalDateTime
import kotlin.math.roundToInt

/**
 * Реализация алгоритма SuperMemo-2 (SM-2) для интервальных повторений.
 * Используется для расчета следующей даты показа карточки.
 */
object SpacedRepetition {

    data class ReviewResult(
        val nextReviewDate: LocalDateTime,
        val newInterval: Int,
        val newEaseFactor: Float,
        val repetitions: Int
    )

    /**
     * Вычисляет новые параметры карточки.
     * @param quality Оценка качества ответа (0 - забыл, 3 - с трудом, 5 - отлично).
     * @param previousInterval Предыдущий интервал в днях.
     * @param previousEaseFactor Предыдущий фактор легкости.
     * @param previousRepetitions Количество успешных повторений подряд.
     */
    fun calculate(
        quality: Int,
        previousInterval: Int,
        previousEaseFactor: Float,
        previousRepetitions: Int
    ): ReviewResult {
        var interval: Int
        var repetitions: Int = previousRepetitions
        var easeFactor: Float = previousEaseFactor

        if (quality >= 3) {
            // Ответ верный (или с небольшим трудом)
            if (repetitions == 0) {
                interval = 1
            } else if (repetitions == 1) {
                interval = 6
            } else {
                interval = (previousInterval * previousEaseFactor).roundToInt()
            }
            repetitions++
        } else {
            // Ответ неверный -> сброс прогресса
            repetitions = 0
            interval = 1
        }

        // Формула SM-2 для Ease Factor:
        // EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        easeFactor += (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)).toFloat()
        if (easeFactor < 1.3f) easeFactor = 1.3f // Минимальный порог EF

        val nextDate = LocalDateTime.now().plusDays(interval.toLong())

        return ReviewResult(
            nextReviewDate = nextDate,
            newInterval = interval,
            newEaseFactor = easeFactor,
            repetitions = repetitions
        )
    }
}