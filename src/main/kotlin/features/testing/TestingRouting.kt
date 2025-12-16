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
    val submitTestUseCase by inject<SubmitTestUseCase>()
    val getTestByLectureUseCase by inject<GetTestByLectureUseCase>()

    authenticate("auth-jwt") {

        // GET Test by Lecture
        get("/api/lectures/{id}/test") {
            val lectureId = call.parameters["id"]?.toIntOrNull() ?: return@get
            val test = getTestByLectureUseCase(lectureId)

            if (test != null) {
                // Маппинг в DTO
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
                call.respond(HttpStatusCode.NotFound, "No test for this lecture")
            }
        }

        // 1. Получить тест по ID темы
        get("/api/topics/{id}/test") {
            val topicId = call.parameters["id"]?.toIntOrNull() ?: return@get
            val test = getTestUseCase(topicId) // Здесь уже вызван shuffle

            if (test != null) {
                val response = TestDto(
                    id = test.id,
                    title = test.title,
                    timeLimit = test.timeLimit, // <--- Передаем лимит
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
                call.respond(HttpStatusCode.NotFound, "Test not found for this topic")
            }
        }

        // 2. Отправить ответы
        post("/api/tests/{id}/submit") {
            val testId = call.parameters["id"]?.toIntOrNull() ?: return@post

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