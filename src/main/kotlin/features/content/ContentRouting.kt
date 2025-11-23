package org.example.features.content

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.domain.usecase.GetDisciplinesUseCase
import org.koin.ktor.ext.inject

fun Route.contentRouting() {
    // Инжектим UseCase (Koin сам найдет реализацию)
    val getDisciplinesUseCase by inject<GetDisciplinesUseCase>()

    route("/api/disciplines") {
        get {
            val disciplines = getDisciplinesUseCase()
            call.respond(disciplines) // Ktor сам превратит List<Discipline> в JSON
        }
    }
}