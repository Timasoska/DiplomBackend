package org.example.features.content

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.data.db.LectureFiles
import org.example.data.dto.UpdateProgressRequest
import org.example.domain.repository.ContentRepository
import org.example.domain.usecase.*
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

fun Route.contentRouting() {
    val favoritesUseCase by inject<FavoritesUseCase>()
    val getDisciplinesUseCase by inject<GetDisciplinesUseCase>()
    val getTopicsUseCase by inject<GetTopicsUseCase>()
    val getLectureUseCase by inject<GetLectureUseCase>()
    val searchUseCase by inject<SearchUseCase>()
    val lectureProgressUseCase by inject<LectureProgressUseCase>() // <--- ИНЖЕКТ

    val contentRepository by inject<ContentRepository>()

    get("/api/files/{id}") {
        val fileId = call.parameters["id"]?.toIntOrNull() ?: return@get

        val fileRow = transaction {
            LectureFiles.select { LectureFiles.id eq fileId }.singleOrNull()
        }

        if (fileRow != null) {
            val file = java.io.File(fileRow[LectureFiles.filePath])
            if (file.exists()) {
                // Заголовок, чтобы браузер скачивал, а не открывал как текст
                val filename = fileRow[LectureFiles.title]
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, filename).toString()
                )
                call.respondFile(file)
            } else {
                call.respond(HttpStatusCode.NotFound, "File not found on disk")
            }
        } else {
            call.respond(HttpStatusCode.NotFound, "File record not found")
        }
    }

    authenticate("auth-jwt") {

        // --- ЛЕКЦИИ И ТЕМЫ ---

        get("/api/disciplines") {
            call.respond(getDisciplinesUseCase())
        }

        get("/api/disciplines/{id}/topics") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid Discipline ID")
                return@get
            }
            call.respond(getTopicsUseCase(id))
        }

        // Обновленный метод (с userId)
        get("/api/topics/{id}/lectures") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid Topic ID")
                return@get
            }

            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asInt() ?: 0

            val lectures = getLectureUseCase.byTopicId(id, userId)
            call.respond(lectures)
        }

        get("/api/lectures/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid Lecture ID")
                return@get
            }

            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asInt()!!

            val lecture = getLectureUseCase.byId(id, userId)
            if (lecture != null) {
                call.respond(lecture)
            } else {
                call.respond(HttpStatusCode.NotFound, "Lecture not found")
            }
        }

        // --- СПРАВОЧНИКИ И ФАЙЛЫ ---

        get("/api/references") {
            val refs = contentRepository.getReferenceMaterials()
            call.respond(refs)
        }

        // --- ПРОГРЕСС И ЗАКЛАДКИ ---

        get("/api/lectures/{id}/progress") {
            val lectureId = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("id")?.asInt()!!

            val progressDto = lectureProgressUseCase.getProgress(userId, lectureId)

            if (progressDto != null) {
                call.respond(progressDto)
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }

        post("/api/lectures/{id}/progress") {
            val lectureId = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest)
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("id")?.asInt()!!

            val request = try {
                call.receive<UpdateProgressRequest>()
            } catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest)
            }

            lectureProgressUseCase.saveProgress(
                userId,
                lectureId,
                request.progressIndex,
                request.quote
            )
            call.respond(HttpStatusCode.OK)
        }

        // --- ПОИСК ---
        get("/api/search") {
            val query = call.request.queryParameters["q"] ?: ""
            call.respond(searchUseCase(query))
        }

        // --- ИЗБРАННОЕ ---

        get("/api/favorites") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("id")?.asInt()!!
            call.respond(favoritesUseCase.getAll(userId))
        }

        post("/api/favorites/{id}") {
            val lectureId = call.parameters["id"]?.toIntOrNull() ?: return@post
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("id")?.asInt()!!
            favoritesUseCase.add(userId, lectureId)
            call.respond(HttpStatusCode.OK)
        }

        delete("/api/favorites/{id}") {
            val lectureId = call.parameters["id"]?.toIntOrNull() ?: return@delete
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("id")?.asInt()!!
            favoritesUseCase.remove(userId, lectureId)
            call.respond(HttpStatusCode.OK)
        }
    }
}