package org.example.data.repository

import org.example.data.db.Users
import org.example.data.db.dbQuery
import org.example.domain.repository.AuthRepository
import org.example.domain.repository.UserRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

class AuthRepositoryImpl : AuthRepository {

    override suspend fun createUser(email: String, passwordHash: String, role: String): Int? = dbQuery {
        try {
            val insertStatement = Users.insert {
                it[Users.email] = email
                it[Users.passwordHash] = passwordHash
                it[Users.role] = role // <--- Пишем роль
            }
            insertStatement[Users.id]
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun findUserByEmail(email: String): UserRow? = dbQuery {
        Users.select { Users.email eq email }
            .map {
                UserRow(
                    id = it[Users.id],
                    email = it[Users.email],
                    passwordHash = it[Users.passwordHash],
                    role = it[Users.role] // <--- Читаем роль
                )
            }
            .singleOrNull()
    }
}