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

fun Route.adminRouting() {
    val importContentUseCase by inject<ImportContentUseCase>()
    val uploadLectureUseCase by inject<UploadLectureUseCase>() // <--- Инжект

    val ADMIN_SECRET = "diploma-secret-key-2025"

    route("/api/admin") {

        // JSON Import (старый метод)
        post("/import") {
            val secret = call.request.header("X-Admin-Secret")
            if (secret != ADMIN_SECRET) {
                call.respond(HttpStatusCode.Forbidden, "Access Denied")
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

        // WORD DOCX Upload (НОВЫЙ МЕТОД)
        post("/upload/docx") {
            // 1. Проверка прав
            if (call.request.header("X-Admin-Secret") != ADMIN_SECRET) {
                call.respond(HttpStatusCode.Forbidden)
                return@post
            }

            // 2. Чтение Multipart
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
                            // Кэшируем поток в память
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

            // 3. Обработка
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