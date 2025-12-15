package org.example.features.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.domain.usecase.LoginUseCase
import org.example.domain.usecase.RegisterUseCase
import org.example.features.auth.model.AuthResponse
import org.example.features.auth.model.LoginRequest
import org.example.features.auth.model.RegisterRequest
import org.koin.ktor.ext.inject

fun Route.authRouting() {
    val registerUseCase by inject<RegisterUseCase>()
    val loginUseCase by inject<LoginUseCase>()

    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            // UseCase теперь возвращает AuthResponse (token + role) или null
            val response = registerUseCase(request)

            if (response != null) {
                call.respond(HttpStatusCode.Created, response)
            } else {
                call.respond(HttpStatusCode.Conflict, "User already exists")
            }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = loginUseCase(request)

            if (response != null) {
                call.respond(HttpStatusCode.OK, response)
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
            }
        }
    }
}