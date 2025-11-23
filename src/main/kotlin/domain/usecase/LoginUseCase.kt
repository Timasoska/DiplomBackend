package org.example.domain.usecase

import org.example.domain.repository.AuthRepository
import org.example.features.auth.model.LoginRequest
import org.example.features.auth.security.PasswordService
import org.example.features.auth.security.TokenService

class LoginUseCase(
    private val repository: AuthRepository,
    private val passwordService: PasswordService,
    private val tokenService: TokenService
) {
    suspend operator fun invoke(request: LoginRequest): String? {
        val user = repository.findUserByEmail(request.email) ?: return null

        if (!passwordService.check(request.password, user.passwordHash)) {
            return null
        }

        return tokenService.generate(user.id, user.email)
    }
}