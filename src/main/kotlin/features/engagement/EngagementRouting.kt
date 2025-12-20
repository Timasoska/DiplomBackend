package org.example.features.engagment

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.data.dto.AddXpRequest
import org.example.domain.repository.ContentRepository
import org.koin.ktor.ext.inject

fun Route.engagementRouting() {
    val contentRepository by inject<ContentRepository>()

    authenticate("auth-jwt") {
        route("/api/engagement") {

            // Получить текущий статус (Стрик, XP, цель)
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asInt()!!

                try {
                    val status = contentRepository.getEngagementStatus(userId)
                    call.respond(status)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
                }
            }

            // Начислить опыт (вызывается клиентом при завершении действия)
            // В идеале, XP за тесты начисляет сервер сам, но для лекций (проскроллил до конца) нужен этот метод.
            post("/xp") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asInt()!!

                val request = try {
                    call.receive<AddXpRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest)
                }

                contentRepository.addXp(userId, request.amount)
                call.respond(HttpStatusCode.OK, "XP Added")
            }
        }
    }
}