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
import org.example.data.dto.TestDto
import org.example.data.dto.QuestionDto
import org.example.domain.usecase.GetTestByLectureUseCase

fun Route.testingRouting() {
    val getTestUseCase by inject<GetTestUseCase>()
    val getTestByLectureUseCase by inject<GetTestByLectureUseCase>()
    val submitTestUseCase by inject<SubmitTestUseCase>()

    authenticate("auth-jwt") {

        // 1. Получить тест по ID ТЕМЫ
        get("/api/topics/{id}/test") {
            val topicId = call.parameters["id"]?.toIntOrNull() ?: return@get
            println("🔍 [API] Requesting test for TOPIC ID: $topicId")

            val test = getTestUseCase(topicId)

            if (test != null) {
                val response = TestDto(
                    id = test.id,
                    title = test.title,
                    timeLimit = test.timeLimit,
                    lectureId = test.lectureId,
                    questions = test.questions.map { q ->
                        QuestionDto(
                            id = q.id,
                            text = q.text,
                            difficulty = q.difficulty,
                            isMultipleChoice = q.isMultipleChoice,
                            answers = q.answers.map { a -> AnswerDto(a.id, a.text) }
                        )
                    }
                )
                call.respond(response)
            } else {
                println("❌ [API] Test not found for TOPIC ID: $topicId")
                call.respond(HttpStatusCode.NotFound, "Test not found for this topic")
            }
        }

        // 2. Получить тест по ID ЛЕКЦИИ
        get("/api/lectures/{id}/test") {
            val lectureId = call.parameters["id"]?.toIntOrNull() ?: return@get
            println("🔍 [API] Requesting test for LECTURE ID: $lectureId")

            val test = getTestByLectureUseCase(lectureId)

            if (test != null) {
                val response = TestDto(
                    id = test.id,
                    title = test.title,
                    timeLimit = test.timeLimit,
                    lectureId = test.lectureId,
                    questions = test.questions.map { q ->
                        QuestionDto(
                            id = q.id,
                            text = q.text,
                            difficulty = q.difficulty,
                            isMultipleChoice = q.isMultipleChoice,
                            answers = q.answers.map { a -> AnswerDto(a.id, a.text) }
                        )
                    }
                )
                call.respond(response)
            } else {
                println("❌ [API] Test not found for LECTURE ID: $lectureId")
                call.respond(HttpStatusCode.NotFound, "No test for this lecture")
            }
        }

        // 3. ОТПРАВИТЬ ОТВЕТЫ (SUBMIT)
        post("/api/tests/{id}/submit") {
            val testId = call.parameters["id"]?.toIntOrNull() ?: return@post

            // Логируем входящие данные
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asInt()!!
            println("🚀 [API] Submitting test ID: $testId by User ID: $userId")

            val userAnswers = try {
                call.receive<List<SubmitAnswerRequest>>()
            } catch (e: Exception) {
                println("❌ [API] Invalid JSON format: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, "Invalid data format")
                return@post
            }

            println("📦 [API] Answers received: ${userAnswers.size}")

            try {
                val result = submitTestUseCase(userId, testId, userAnswers)
                println("✅ [API] Test submitted successfully. Score: ${result.score}")
                call.respond(result)
            } catch (e: Exception) {
                // ВОТ ЭТО САМОЕ ВАЖНОЕ: Логируем реальную причину ошибки 500
                println("🔥 [API ERROR] Submit failed:")
                e.printStackTrace() // Пишет полный стек ошибки в консоль Docker
                call.respond(HttpStatusCode.InternalServerError, "Server error: ${e.localizedMessage}")
            }
        }
    }
}