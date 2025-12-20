package org.example.features.flashcards

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.data.dto.ReviewFlashcardRequest
import org.example.domain.repository.ContentRepository
import org.koin.ktor.ext.inject

fun Route.flashcardRouting() {
    val contentRepository by inject<ContentRepository>()

    authenticate("auth-jwt") {
        route("/api/flashcards") {

            // Получить стопку карточек на сегодня
            get("/due") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asInt()!!

                try {
                    val cards = contentRepository.getDueFlashcards(userId)
                    call.respond(cards)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
                }
            }

            // Отправить результат повторения (Нажал "Легко", "Сложно" и т.д.)
            post("/review") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asInt()!!

                val request = try {
                    call.receive<ReviewFlashcardRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest)
                }

                contentRepository.saveFlashcardReview(userId, request.questionId, request.quality)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}