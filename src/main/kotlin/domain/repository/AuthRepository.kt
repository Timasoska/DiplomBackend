package org.example.domain.repository

import org.example.data.db.Users

interface AuthRepository {
    suspend fun createUser(email: String, passwordHash: String): Int?
    suspend fun findUserByEmail(email: String): UserRow?
}

// Вспомогательная модель для передачи данных из БД
data class UserRow(
    val id: Int,
    val email: String,
    val passwordHash: String
)