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
import org.example.domain.repository.ContentRepository


/**
 * Роутинг для управления группами и получения аналитики.
 * Реализует функционал как для преподавателей (управление, отчеты),
 * так и для студентов (вступление, список участников).
 */
fun Route.groupRouting() {
    val createGroupUseCase by inject<CreateGroupUseCase>()
    val joinGroupUseCase by inject<JoinGroupUseCase>()
    val getTeacherGroupsUseCase by inject<GetTeacherGroupsUseCase>()
    val getAnalyticsUseCase by inject<GetAnalyticsUseCase>()
    val updateGroupUseCase by inject<UpdateGroupUseCase>()
    val deleteGroupUseCase by inject<DeleteGroupUseCase>()
    val removeStudentUseCase by inject<RemoveStudentUseCase>()
    val contentRepository by inject<ContentRepository>()

    authenticate("auth-jwt") {
        route("/api/groups") {

            // --- ДЕТАЛЬНЫЙ ОТЧЕТ ПО СТУДЕНТУ (Deep Analytics) ---
            get("/{groupId}/student/{studentId}/report") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                // Проверка прав доступа
                if (role != "teacher") {
                    println("🚫 [AUTH] Access denied to report for role: $role")
                    return@get call.respond(HttpStatusCode.Forbidden, "Access Denied: Teachers only")
                }

                val groupId = call.parameters["groupId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val studentId = call.parameters["studentId"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)

                println("🔍 [DEBUG] Teacher is requesting report for student $studentId in group $groupId")

                try {
                    // Используем метод репозитория (внутри которого есть dbQuery),
                    // чтобы избежать ошибки "No transaction in context"
                    val disciplineId = contentRepository.getDisciplineIdByGroupId(groupId)
                        ?: return@get call.respond(HttpStatusCode.NotFound, "Group or Discipline not found")

                    val report = contentRepository.getStudentDetailedReport(studentId, disciplineId)
                    call.respond(HttpStatusCode.OK, report)
                } catch (e: Exception) {
                    println("🔥 [API ERROR] Failed to build student report: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, e.localizedMessage ?: "Internal Server Error")
                }
            }

            // СТУДЕНТ: Список участников группы
            get("/{id}/members") {
                val groupId = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                try {
                    val members = contentRepository.getGroupMembers(groupId)
                    call.respond(members)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error fetching members")
                }
            }

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
                    call.respond(HttpStatusCode.NotFound, result.exceptionOrNull()?.message ?: "Group not found")
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

            // УЧИТЕЛЬ: Аналитика рисков по группе
            get("/{id}/analytics") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != "teacher") return@get call.respond(HttpStatusCode.Forbidden)

                val groupId = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)

                val analytics = getAnalyticsUseCase.getAnalytics(groupId)
                call.respond(analytics)
            }

            // УЧИТЕЛЬ: Изменить название группы
            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()
                if (role != "teacher") return@put call.respond(HttpStatusCode.Forbidden)

                val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UpdateGroupRequest>()

                updateGroupUseCase.updateGroup(id, request.name)
                call.respond(HttpStatusCode.OK)
            }

            // УЧИТЕЛЬ: Удалить группу
            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()
                if (role != "teacher") return@delete call.respond(HttpStatusCode.Forbidden)

                val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)

                deleteGroupUseCase.deleteGroup(id)
                call.respond(HttpStatusCode.OK)
            }

            // УЧИТЕЛЬ: Удалить студента из группы
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