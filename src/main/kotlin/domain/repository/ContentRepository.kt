package org.example.domain.repository

import org.example.domain.model.Discipline

interface ContentRepository {
    suspend fun getAllDisciplines(): List<Discipline>
}