package org.example.domain.repository

import org.example.data.db.Users

interface AuthRepository {
    // Добавили аргумент name
    suspend fun createUser(email: String, passwordHash: String, role: String, name: String): Int?
    suspend fun findUserByEmail(email: String): UserRow?
}

// Добавили поле name в DTO базы данных
data class UserRow(
    val id: Int,
    val email: String,
    val passwordHash: String,
    val role: String,
    val name: String // <--- ВОТ ЭТО ПОЛЕ НУЖНО ДОБАВИТЬ
)