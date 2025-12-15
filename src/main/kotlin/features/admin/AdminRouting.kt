package org.example.features.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.data.loader.SeedDiscipline
import org.example.domain.usecase.ImportContentUseCase
import org.koin.ktor.ext.inject

fun Route.adminRouting() {
    val importContentUseCase by inject<ImportContentUseCase>()

    // Секретный ключ (в реальном проекте хранить в конфиге)
    val ADMIN_SECRET = "diploma-secret-key-2025"

    route("/api/admin") {

        post("/import") {
            // 1. Проверка безопасности
            val secret = call.request.header("X-Admin-Secret")
            if (secret != ADMIN_SECRET) {
                call.respond(HttpStatusCode.Forbidden, "Access Denied")
                return@post
            }

            // 2. Получение JSON
            val data = try {
                call.receive<List<SeedDiscipline>>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid JSON format: ${e.message}")
                return@post
            }

            // 3. Импорт
            try {
                importContentUseCase(data)
                call.respond(HttpStatusCode.OK, "Content imported successfully! Added ${data.size} disciplines.")
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, "Import failed: ${e.message}")
            }
        }
    }
}