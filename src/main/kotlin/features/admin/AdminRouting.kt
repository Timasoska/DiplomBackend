package org.example.features.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.data.loader.SeedDiscipline
import org.koin.ktor.ext.inject
import io.ktor.http.content.*
import java.io.InputStream
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.example.data.dto.ReferenceMaterialDto
import org.example.data.dto.SaveTestRequest
import org.example.data.dto.SaveTopicRequest
import org.example.data.dto.UpdateLectureRequest
import org.example.domain.repository.ContentRepository
import org.example.domain.usecase.*


fun Route.adminRouting() {
    val importContentUseCase by inject<ImportContentUseCase>()
    val uploadLectureUseCase by inject<UploadLectureUseCase>()
    val updateLectureUseCase by inject<UpdateLectureUseCase>() // <--- Инжект
    val deleteLectureUseCase by inject<DeleteLectureUseCase>() // <--- Инжект
    val saveTestUseCase by inject<SaveTestUseCase>() // <--- Инжект
    val getAdminTestUseCase by inject<GetAdminTestUseCase>() // <--- Инжект
    val contentRepository by inject<ContentRepository>()
    val saveTopicUseCase by inject<SaveTopicUseCase>() // <--- Инжект


    // ЗАЩИТА: Доступ только с валидным токеном
    authenticate("auth-jwt") {

        route("/api/admin") {

            // 1. ДОБАВИТЬ СПРАВОЧНЫЙ МАТЕРИАЛ
            post("/references") {
                val principal = call.principal<JWTPrincipal>()
                if (principal?.payload?.getClaim("role")?.asString() != "teacher") {
                    call.respond(HttpStatusCode.Forbidden)
                    return@post
                }

                // Используем DTO или простую Map, для скорости возьмем DTO
                // (Придется добавить этот класс в DTO, если его нет для реквеста,
                //  или примем параметры). Давай примем параметры формы.
                val params = call.receive<ReferenceMaterialDto>()
                // (Мы используем DTO и для запроса, и для ответа, это ок для диплома)

                contentRepository.addReferenceMaterial(params.title, params.url)
                call.respond(HttpStatusCode.OK, "Added")
            }

            // 2. ПРИКРЕПИТЬ ФАЙЛ К ЛЕКЦИИ
            post("/lectures/{id}/files") {
                val principal = call.principal<JWTPrincipal>()
                if (principal?.payload?.getClaim("role")?.asString() != "teacher") {
                    call.respond(HttpStatusCode.Forbidden)
                    return@post
                }

                val lectureId = call.parameters["id"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest)

                val multipart = call.receiveMultipart()
                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        val fileName = part.originalFileName ?: "file"

                        // Сохраняем файл в папку "uploads" внутри проекта
                        val uploadDir = java.io.File("uploads")
                        if (!uploadDir.exists()) uploadDir.mkdirs()

                        val file = java.io.File(uploadDir, "${System.currentTimeMillis()}_$fileName")

                        part.streamProvider().use { input ->
                            file.outputStream().use { output -> input.copyTo(output) }
                        }

                        // Сохраняем путь в БД
                        contentRepository.attachFileToLecture(lectureId, fileName, file.absolutePath)
                    }
                    part.dispose()
                }
                call.respond(HttpStatusCode.OK, "File attached")
            }

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

            post("/lectures/{id}/files") {
                // Проверка учителя...
                val lectureId = call.parameters["id"]?.toIntOrNull() ?: return@post

                val multipart = call.receiveMultipart()
                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        val fileName = part.originalFileName ?: "file"
                        // Сохраняем в папку uploads
                        val file = java.io.File("uploads/$fileName")
                        file.parentFile.mkdirs()
                        part.streamProvider().use { input -> file.outputStream().use { output -> input.copyTo(output) } }

                        // Пишем в БД
                        contentRepository.attachFileToLecture(lectureId, fileName, file.absolutePath)
                    }
                    part.dispose()
                }
                call.respond(HttpStatusCode.OK)
            }

            // РЕДАКТИРОВАНИЕ ЛЕКЦИИ
            put("/lectures/{id}") {
                // 1. Проверка роли
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != "teacher") {
                    call.respond(HttpStatusCode.Forbidden, "Access Denied")
                    return@put
                }

                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest)

                val request = try {
                    call.receive<UpdateLectureRequest>()
                } catch (e: Exception) {
                    return@put call.respond(HttpStatusCode.BadRequest)
                }

                updateLectureUseCase(id, request.title, request.content)
                call.respond(HttpStatusCode.OK, "Lecture updated")
            }

            // УДАЛЕНИЕ
            delete("/lectures/{id}") {
                // 1. Проверка роли
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != "teacher") {
                    call.respond(HttpStatusCode.Forbidden, "Access Denied")
                    return@delete
                }

                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)

                try {
                    deleteLectureUseCase(id)
                    call.respond(HttpStatusCode.OK, "Lecture deleted")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
                }
            }

            // СОХРАНЕНИЕ ТЕСТА (Создание или Обновление)
            post("/tests") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != "teacher") {
                    call.respond(HttpStatusCode.Forbidden, "Access Denied")
                    return@post
                }

                val request = try {
                    call.receive<SaveTestRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid JSON: ${e.message}")
                    return@post
                }

                try {
                    saveTestUseCase(request)
                    call.respond(HttpStatusCode.OK, "Test saved successfully")
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, "Error saving test: ${e.message}")
                }
            }
            // ПОЛУЧЕНИЕ ТЕСТА ДЛЯ РЕДАКТИРОВАНИЯ (С ответами)
            get("/tests/{topicId}") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != "teacher") {
                    call.respond(HttpStatusCode.Forbidden, "Access Denied")
                    return@get
                }

                val topicId = call.parameters["topicId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

                val test = getAdminTestUseCase(topicId)
                if (test != null) {
                    call.respond(test)
                } else {
                    call.respond(HttpStatusCode.NoContent) // Теста нет
                }
            }

            get("/tests/lecture/{lectureId}") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != "teacher") {
                    call.respond(HttpStatusCode.Forbidden, "Access Denied")
                    return@get
                }

                val lectureId = call.parameters["lectureId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

                // Вызываем UseCase с именованным параметром lectureId
                val test = getAdminTestUseCase(lectureId = lectureId)

                if (test != null) {
                    call.respond(test)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            // СОЗДАНИЕ ТЕМЫ
            post("/topics") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != "teacher") {
                    call.respond(HttpStatusCode.Forbidden, "Access Denied")
                    return@post
                }

                val request = try {
                    call.receive<SaveTopicRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid JSON")
                    return@post
                }

                saveTopicUseCase(request.disciplineId, request.name)
                call.respond(HttpStatusCode.OK, "Topic created")
            }

        }
    }
}