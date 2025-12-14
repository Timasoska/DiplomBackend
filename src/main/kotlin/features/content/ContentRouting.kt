package org.example.features.content

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.data.dto.UpdateProgressRequest
import org.example.domain.usecase.*
import org.koin.ktor.ext.inject

fun Route.contentRouting() {
    val favoritesUseCase by inject<FavoritesUseCase>()
    val getDisciplinesUseCase by inject<GetDisciplinesUseCase>()
    val getTopicsUseCase by inject<GetTopicsUseCase>()
    val getLectureUseCase by inject<GetLectureUseCase>()
    val searchUseCase by inject<SearchUseCase>()
    val lectureProgressUseCase by inject<LectureProgressUseCase>() // <--- ИНЖЕКТ

    authenticate("auth-jwt") {
        // 1. Получить позицию
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

        // 2. Сохранить позицию
        post("/api/lectures/{id}/progress") {
            val lectureId = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest)
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("id")?.asInt()!!

            val request = try {
                call.receive<UpdateProgressRequest>()
            } catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest)
            }

            // Передаем и индекс, и цитату
            lectureProgressUseCase.saveProgress(
                userId,
                lectureId,
                request.progressIndex,
                request.quote
            )
            call.respond(HttpStatusCode.OK)
        }

        // Поиск
        get("/api/search") {
            val query = call.request.queryParameters["q"] ?: ""
            val results = searchUseCase(query)
            call.respond(results)
        }

        // 1. Все дисциплины
        get("/api/disciplines") {
            val disciplines = getDisciplinesUseCase()
            call.respond(disciplines)
        }

        // 2. Темы конкретной дисциплины (например: /api/disciplines/1/topics)
        get("/api/disciplines/{id}/topics") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid Discipline ID")
                return@get
            }
            val topics = getTopicsUseCase(id)
            call.respond(topics)
        }

        // 3. Лекции по теме
        get("/api/topics/{id}/lectures") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid Topic ID")
                return@get
            }
            val lectures = getLectureUseCase.byTopicId(id)
            call.respond(lectures)
        }

        // 4. Конкретная лекция
        get("/api/lectures/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            // Достаем UserID из токена
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asInt()!!

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid Lecture ID")
                return@get
            }

            // Передаем userId
            val lecture = getLectureUseCase.byId(id, userId)

            if (lecture != null) {
                call.respond(lecture)
            } else {
                call.respond(HttpStatusCode.NotFound, "Lecture not found")
            }
        }

        // 1. Добавить в избранное
        post("/api/favorites/{id}") {
            val lectureId = call.parameters["id"]?.toIntOrNull() ?: return@post

            // Достаем ID пользователя из токена
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asInt()

            if (userId != null) {
                favoritesUseCase.add(userId, lectureId)
                call.respond(HttpStatusCode.OK, "Added to favorites")
            } else {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }

        // 2. Удалить из избранного
        delete("/api/favorites/{id}") {
            val lectureId = call.parameters["id"]?.toIntOrNull() ?: return@delete
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asInt()

            if (userId != null) {
                favoritesUseCase.remove(userId, lectureId)
                call.respond(HttpStatusCode.OK, "Removed from favorites")
            }
        }

        // 3. Получить список избранного
        get("/api/favorites") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asInt()

            if (userId != null) {
                val favorites = favoritesUseCase.getAll(userId)
                call.respond(favorites)
            } else {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }

    }
}