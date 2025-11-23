package org.example.domain.usecase

import org.example.domain.model.Discipline
import org.example.domain.repository.ContentRepository


class GetDisciplinesUseCase(private val repository: ContentRepository) {
    // Оператор invoke позволяет вызывать класс как функцию
    suspend operator fun invoke(): List<Discipline> {
        return repository.getAllDisciplines()
    }
}