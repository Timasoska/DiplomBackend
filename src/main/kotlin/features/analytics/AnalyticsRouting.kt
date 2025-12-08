package org.example.features.analytics

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.domain.usecase.GetProgressUseCase
import org.example.domain.usecase.GetRecommendationsUseCase
import org.koin.ktor.ext.inject

fun Route.analyticsRouting() {
    val getProgressUseCase by inject<GetProgressUseCase>()
    val getRecommendationsUseCase by inject<GetRecommendationsUseCase>()

    authenticate("auth-jwt") {
        route("/api/analytics") {

            get("/progress") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asInt()!!

                val progress = getProgressUseCase(userId)
                call.respond(progress)
            }

            get("/recommendations") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")?.asInt()!!

                val topics = getRecommendationsUseCase(userId)
                call.respond(topics)
            }
        }
    }
}