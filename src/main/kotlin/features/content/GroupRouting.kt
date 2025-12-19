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
import org.example.data.dto.UpdateGroupRequest
import org.example.domain.usecase.*
import org.koin.ktor.ext.inject

fun Route.groupRouting() {
    // Инжектим каждый UseCase отдельно
    val createGroupUseCase by inject<CreateGroupUseCase>()
    val joinGroupUseCase by inject<JoinGroupUseCase>()
    val getTeacherGroupsUseCase by inject<GetTeacherGroupsUseCase>()
    val getAnalyticsUseCase by inject<GetAnalyticsUseCase>()

    // Новые инжекты
    val updateGroupUseCase by inject<UpdateGroupUseCase>()
    val deleteGroupUseCase by inject<DeleteGroupUseCase>()
    val removeStudentUseCase by inject<RemoveStudentUseCase>()

    authenticate("auth-jwt") {
        route("/api/groups") {

            // СТУДЕНТ: Вступить в группу
            post("/join") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asInt()!!

                val request = try { call.receive<JoinGroupRequest>() } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest)
                }

                val result = joinGroupUseCase.joinGroup(userId, request.inviteCode)
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
                val role = principal.payload.getClaim("role")?.asString()

                if (role != "teacher") return@post call.respond(HttpStatusCode.Forbidden)

                val request = call.receive<CreateGroupRequest>()
                val code = createGroupUseCase.createGroup(userId, request.disciplineId, request.name)

                call.respond(HttpStatusCode.Created, mapOf("inviteCode" to code))
            }

            // УЧИТЕЛЬ: Получить свои группы
            get("/my") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asInt()!!
                val role = principal.payload.getClaim("role")?.asString()

                if (role != "teacher") return@get call.respond(HttpStatusCode.Forbidden)

                val groups = getTeacherGroupsUseCase.getTeacherGroups(userId)
                call.respond(groups)
            }

            // УЧИТЕЛЬ: Аналитика по группе
            get("/{id}/analytics") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != "teacher") return@get call.respond(HttpStatusCode.Forbidden)

                val groupId = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)

                val analytics = getAnalyticsUseCase.getAnalytics(groupId)
                call.respond(analytics)
            }

            // --- НОВЫЕ МЕТОДЫ ---

            // 1. Изменить название группы
            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()
                if (role != "teacher") return@put call.respond(HttpStatusCode.Forbidden)

                val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UpdateGroupRequest>()

                updateGroupUseCase.updateGroup(id, request.name)

                call.respond(HttpStatusCode.OK)
            }

            // 2. Удалить группу
            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()
                if (role != "teacher") return@delete call.respond(HttpStatusCode.Forbidden)

                val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)

                deleteGroupUseCase.deleteGroup(id)
                call.respond(HttpStatusCode.OK)
            }

            // 3. Удалить студента из группы
            delete("/{groupId}/students/{studentId}") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()
                if (role != "teacher") return@delete call.respond(HttpStatusCode.Forbidden)

                val groupId = call.parameters["groupId"]?.toIntOrNull() ?: return@delete
                val studentId = call.parameters["studentId"]?.toIntOrNull() ?: return@delete

                removeStudentUseCase.removeStudent(groupId, studentId)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}