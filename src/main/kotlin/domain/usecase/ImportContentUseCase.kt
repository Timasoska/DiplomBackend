package org.example.domain.usecase

import org.example.data.loader.SeedDiscipline
import org.example.domain.repository.ContentRepository

class ImportContentUseCase(private val repository: ContentRepository) {
    suspend operator fun invoke(data: List<SeedDiscipline>) {
        repository.importContent(data)
    }
}