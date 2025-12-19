package org.example.features.content


import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.data.dto.CreateGroupRequest
import org.example.data.dto.JoinGroupRequest
import org.example.domain.usecase.GroupUseCase
import org.koin.ktor.ext.inject

fun Route.groupRouting() {
    val groupUseCase by inject<GroupUseCase>()

    authenticate("auth-jwt") {
        route("/api/groups") {

            // СТУДЕНТ: Вступить в группу
            post("/join") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asInt()!!
                val role = principal.payload.getClaim("role").asString()

                // (Опционально) Проверка, что это студент, хотя учителю тоже можно для теста

                val request = try { call.receive<JoinGroupRequest>() } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest)
                }

                val result = groupUseCase.joinGroup(userId, request.inviteCode)
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, "Joined successfully")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Group not found or already joined")
                }
            }

            // УЧИТЕЛЬ: Создать группу
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asInt()!!
                val role = principal.payload.getClaim("role").asString()

                if (role != "teacher") {
                    return@post call.respond(HttpStatusCode.Forbidden)
                }

                // ИСПРАВЛЕНИЕ: Указываем правильный DTO класс
                val request = call.receive<CreateGroupRequest>() // <--- CreateGroupRequest

                val code = groupUseCase.createGroup(userId, request.disciplineId, request.name)

                call.respond(HttpStatusCode.Created, mapOf("inviteCode" to code))
            }

            // УЧИТЕЛЬ: Получить свои группы
            get("/my") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asInt()!!
                val role = principal.payload.getClaim("role").asString()

                if (role != "teacher") {
                    return@get call.respond(HttpStatusCode.Forbidden)
                }

                val groups = groupUseCase.getTeacherGroups(userId)
                call.respond(groups)
            }

            // УЧИТЕЛЬ: Аналитика по группе
            get("/{id}/analytics") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()
                if (role != "teacher") return@get call.respond(HttpStatusCode.Forbidden)

                val groupId = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)

                val analytics = groupUseCase.getGroupAnalytics(groupId)
                call.respond(analytics)
            }
        }
    }
}