package org.example.data.repository


import org.example.data.db.Disciplines
import org.example.data.db.dbQuery
import org.example.domain.model.Discipline
import org.example.domain.repository.ContentRepository
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.selectAll

class ContentRepositoryImpl : ContentRepository {
    override suspend fun getAllDisciplines(): List<Discipline> = dbQuery {
        Disciplines.selectAll().map { row ->
            Discipline(
                id = row[Disciplines.id],
                name = row[Disciplines.name],
                description = row[Disciplines.description]
            )
        }
    }
}