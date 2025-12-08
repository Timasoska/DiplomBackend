package org.example.features.testing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.domain.model.AnswerDto
import org.example.domain.model.SubmitAnswerRequest
import org.example.domain.usecase.GetTestUseCase
import org.example.domain.usecase.SubmitTestUseCase
import org.koin.ktor.ext.inject

fun Route.testingRouting() {
    val getTestUseCase by inject<GetTestUseCase>()
    val submitTestUseCase by inject<SubmitTestUseCase>()

    authenticate("auth-jwt") {

        // 1. Получить тест по ID темы
        get("/api/topics/{id}/test") {
            val topicId = call.parameters["id"]?.toIntOrNull() ?: return@get
            val test = getTestUseCase(topicId)

            if (test != null) {
                // ВАЖНО: Преобразуем ответы в DTO, чтобы не отправить isCorrect=true
                val safeQuestions = test.questions.map { q ->
                    // Копируем вопрос, подменяя список ответов на безопасный (Any/DTO хак для JSON)
                    // Для простоты JSON сериализатора, лучше просто отдадим структуру,
                    // где answer - это AnswerDto
                    mapOf(
                        "id" to q.id,
                        "text" to q.text,
                        "answers" to q.answers.map { a -> AnswerDto(a.id, a.text) }
                    )
                }

                val response = mapOf(
                    "id" to test.id,
                    "title" to test.title,
                    "questions" to safeQuestions
                )

                call.respond(response)
            } else {
                call.respond(HttpStatusCode.NotFound, "Test not found for this topic")
            }
        }

        // 2. Отправить ответы
        post("/api/tests/{id}/submit") {
            val testId = call.parameters["id"]?.toIntOrNull() ?: return@post
            val userAnswers = call.receive<List<SubmitAnswerRequest>>()

            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asInt()!!

            val result = submitTestUseCase(userId, testId, userAnswers)
            call.respond(result)
        }
    }
}