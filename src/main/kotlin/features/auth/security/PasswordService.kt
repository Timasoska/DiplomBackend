package org.example.features.auth.security

import at.favre.lib.crypto.bcrypt.BCrypt

class PasswordService {
    fun hash(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    fun check(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }
}