package org.example.features.testing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable // <--- Не забудь импорт
import org.example.domain.model.AnswerDto
import org.example.domain.model.SubmitAnswerRequest
import org.example.domain.usecase.GetTestUseCase
import org.example.domain.usecase.SubmitTestUseCase
import org.koin.ktor.ext.inject

// --- DTO классы для отправки теста клиенту ---
@Serializable
data class QuestionDto(
    val id: Int,
    val text: String,
    val answers: List<AnswerDto>
)

@Serializable
data class TestDto(
    val id: Int,
    val title: String,
    val questions: List<QuestionDto>
)
// ---------------------------------------------

fun Route.testingRouting() {
    val getTestUseCase by inject<GetTestUseCase>()
    val submitTestUseCase by inject<SubmitTestUseCase>()

    authenticate("auth-jwt") {

        // 1. Получить тест по ID темы
        get("/api/topics/{id}/test") {
            val topicId = call.parameters["id"]?.toIntOrNull() ?: return@get
            val test = getTestUseCase(topicId)

            if (test != null) {
                // Преобразуем Domain Model в DTO (безопасный JSON)
                val response = TestDto(
                    id = test.id,
                    title = test.title,
                    questions = test.questions.map { q ->
                        QuestionDto(
                            id = q.id,
                            text = q.text,
                            answers = q.answers.map { a -> AnswerDto(a.id, a.text) }
                        )
                    }
                )

                call.respond(response)
            } else {
                call.respond(HttpStatusCode.NotFound, "Test not found for this topic")
            }
        }

        // 2. Отправить ответы
        post("/api/tests/{id}/submit") {
            val testId = call.parameters["id"]?.toIntOrNull() ?: return@post

            // Ловим возможные ошибки парсинга JSON от клиента
            val userAnswers = try {
                call.receive<List<SubmitAnswerRequest>>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid data format")
                return@post
            }

            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asInt()!!

            val result = submitTestUseCase(userId, testId, userAnswers)
            call.respond(result)
        }
    }
}