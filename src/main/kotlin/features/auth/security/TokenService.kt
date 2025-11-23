package org.example.features.auth.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

class TokenService {
    // В реальном проекте эти данные хранят в application.conf
    private val secret = "my-super-secret-key"
    private val issuer = "law-navigator"
    private val validityInMs = 36_000_00 * 24 // 24 часа

    fun generate(userId: Int, email: String): String {
        return JWT.create()
            .withAudience(issuer)
            .withIssuer(issuer)
            .withClaim("id", userId)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
            .sign(Algorithm.HMAC256(secret))
    }
}