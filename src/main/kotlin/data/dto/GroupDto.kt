package org.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateGroupRequest(
    val name: String,
    val disciplineId: Int
)

@Serializable
data class JoinGroupRequest(
    val inviteCode: String
)

@Serializable
data class TeacherGroupDto(
    val id: Int,
    val name: String,
    val disciplineName: String,
    val inviteCode: String,
    val studentCount: Int
)

/**
 * Статистика по конкретному студенту в группе.
 * Используется для Risk Clustering.
 */
@Serializable
data class StudentRiskDto(
    val studentId: Int,
    val email: String,
    val averageScore: Double,
    val trend: Double, // Коэффициент наклона (+ растет, - падает)
    val riskLevel: RiskLevel // Вычисленный кластер
)

enum class RiskLevel {
    GREEN,   // Всё отлично (Высокий балл, позитивный тренд)
    YELLOW,  // Внимание (Средний балл, стагнация)
    RED      // Тревога (Низкий балл или резкое падение)
}