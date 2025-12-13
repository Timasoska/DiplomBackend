package org.example.domain.usecase

import org.example.data.dto.LeaderboardItemDto
import org.example.domain.repository.ContentRepository

class GetLeaderboardUseCase(private val repository: ContentRepository) {
    suspend operator fun invoke(): List<LeaderboardItemDto> {
        return repository.getLeaderboard()
    }
}