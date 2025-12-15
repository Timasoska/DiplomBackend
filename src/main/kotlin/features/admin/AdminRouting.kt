package org.example.features.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.data.loader.SeedDiscipline
import org.example.domain.usecase.ImportContentUseCase
import org.koin.ktor.ext.inject
import io.ktor.http.content.*
import org.example.domain.usecase.UploadLectureUseCase
import java.io.InputStream
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*



fun Route.adminRouting() {
    val importContentUseCase by inject<ImportContentUseCase>()
    val uploadLectureUseCase by inject<UploadLectureUseCase>()

    // ЗАЩИТА: Доступ только с валидным токеном
    authenticate("auth-jwt") {

        route("/api/admin") {

            // 1. JSON Import
            post("/import") {
                // ПРОВЕРКА РОЛИ
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != "teacher") {
                    call.respond(HttpStatusCode.Forbidden, "Access Denied: Teachers only")
                    return@post
                }

                val data = try {
                    call.receive<List<SeedDiscipline>>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid JSON format: ${e.message}")
                    return@post
                }

                try {
                    importContentUseCase(data)
                    call.respond(HttpStatusCode.OK, "Content imported successfully!")
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, "Import failed: ${e.message}")
                }
            }

            // 2. WORD DOCX Upload
            post("/upload/docx") {
                // ПРОВЕРКА РОЛИ
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != "teacher") {
                    call.respond(HttpStatusCode.Forbidden, "Access Denied: Teachers only")
                    return@post
                }

                val multipart = call.receiveMultipart()
                var title = ""
                var topicId = 0
                var fileStream: InputStream? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            if (part.name == "title") title = part.value
                            if (part.name == "topicId") topicId = part.value.toIntOrNull() ?: 0
                        }
                        is PartData.FileItem -> {
                            if (part.name == "file") {
                                fileStream = part.streamProvider().use { input ->
                                    input.readBytes().inputStream()
                                }
                            }
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                if (topicId == 0 || fileStream == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing topicId or file")
                    return@post
                }

                try {
                    uploadLectureUseCase(
                        topicId = topicId,
                        title = title.ifBlank { "Загруженная лекция" },
                        fileStream = fileStream!!
                    )
                    call.respond(HttpStatusCode.OK, "File converted and saved successfully!")
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, "Error processing file: ${e.message}")
                }
            }
        }
    }
}