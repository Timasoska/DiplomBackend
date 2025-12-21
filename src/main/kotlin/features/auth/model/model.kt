package org.example.features.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val inviteCode: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val role: String,
    val name: String // <--- НОВОЕ ПОЛЕ: Чтобы клиент знал свое имя
)