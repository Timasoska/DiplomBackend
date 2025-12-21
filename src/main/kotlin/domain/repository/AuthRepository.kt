package org.example.domain.repository

import org.example.data.db.Users

interface AuthRepository {
    suspend fun createUser(email: String, passwordHash: String, role: String, name: String): Int?
    suspend fun findUserByEmail(email: String): UserRow?
}

data class UserRow(
    val id: Int,
    val email: String,
    val passwordHash: String,
    val role: String, // <--- Добавили
    val name: String // <--- Добавили
)