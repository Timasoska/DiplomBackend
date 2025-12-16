package org.example.features.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val inviteCode: String? = null // Если ввести спец код - станешь учителем
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val role: String // Возвращаем роль клиенту, чтобы он знал, какой UI рисовать
)