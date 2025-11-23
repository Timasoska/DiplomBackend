package org.example.domain.usecase

import org.example.domain.repository.AuthRepository
import org.example.features.auth.model.RegisterRequest
import org.example.features.auth.security.PasswordService
import org.example.features.auth.security.TokenService

class RegisterUseCase(
    private val repository: AuthRepository,
    private val passwordService: PasswordService,
    private val tokenService: TokenService
) {
    suspend operator fun invoke(request: RegisterRequest): String? {
        val passwordHash = passwordService.hash(request.password)
        val userId = repository.createUser(request.email, passwordHash) ?: return null
        return tokenService.generate(userId, request.email)
    }
}