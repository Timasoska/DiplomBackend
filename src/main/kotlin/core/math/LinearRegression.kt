package org.example.core.math

object LinearRegression {

    /**
     * Вычисляет коэффициент наклона (Trend) методом наименьших квадратов.
     * @param data Список пар (Номер попытки, Оценка).
     * @return Коэффициент наклона (a). Если > 0 - рост, < 0 - спад.
     */
    fun calculateTrend(data: List<Int>): Double {
        val n = data.size
        if (n < 2) return 0.0 // Недостаточно данных для тренда

        // x - это порядковый номер попытки (0, 1, 2...)
        // y - это оценка (data[x])

        var sumX = 0.0
        var sumY = 0.0
        var sumXY = 0.0
        var sumX2 = 0.0

        for (i in 0 until n) {
            val x = i.toDouble()
            val y = data[i].toDouble()

            sumX += x
            sumY += y
            sumXY += x * y
            sumX2 += x * x
        }

        val numerator = n * sumXY - sumX * sumY
        val denominator = n * sumX2 - sumX * sumX

        if (denominator == 0.0) return 0.0

        return numerator / denominator
    }
}