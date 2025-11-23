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
            val token = registerUseCase(request)

            if (token != null) {
                call.respond(HttpStatusCode.Created, AuthResponse(token))
            } else {
                call.respond(HttpStatusCode.Conflict, "User already exists")
            }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val token = loginUseCase(request)

            if (token != null) {
                call.respond(HttpStatusCode.OK, AuthResponse(token))
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
            }
        }
    }
}