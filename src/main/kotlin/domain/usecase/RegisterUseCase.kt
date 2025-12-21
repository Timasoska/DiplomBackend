package org.example.domain.usecase

import org.example.domain.repository.AuthRepository
import org.example.features.auth.model.AuthResponse
import org.example.features.auth.model.RegisterRequest
import org.example.features.auth.security.PasswordService
import org.example.features.auth.security.TokenService

class RegisterUseCase(
    private val repository: AuthRepository,
    private val passwordService: PasswordService,
    private val tokenService: TokenService
) {
    // Секретный код для регистрации преподавателя (в реале - в конфиг)
    private val TEACHER_INVITE_CODE = "TEACHER2025"

    suspend operator fun invoke(request: RegisterRequest): AuthResponse? {
        val role = if (request.inviteCode == TEACHER_INVITE_CODE) "teacher" else "student"
        val passwordHash = passwordService.hash(request.password)

        // Передаем request.name
        val userId = repository.createUser(request.email, passwordHash, role, request.name) ?: return null

        val token = tokenService.generate(userId, request.email, role)
        return AuthResponse(token, role)
    }
}