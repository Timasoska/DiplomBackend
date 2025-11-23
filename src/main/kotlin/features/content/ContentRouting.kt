package org.example.features.content

import io.ktor.server.application.*
import io.ktor.server.auth.* // Импорт authenticate
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.domain.usecase.GetDisciplinesUseCase
import org.koin.ktor.ext.inject

fun Route.contentRouting() {
    val getDisciplinesUseCase by inject<GetDisciplinesUseCase>()

    // Оборачиваем роуты в authenticate
    authenticate("auth-jwt") {

        route("/api/disciplines") {
            get {
                val disciplines = getDisciplinesUseCase()
                call.respond(disciplines)
            }
        }

    }
}