// File: app/src/main/java/com/example/gigwork/domain/models/AuthState.kt
package com.example.gigwork.domain.models

data class AuthState(
    val token: String = "",
    val userId: String = "",
    val userType: String = ""
) {
    fun isValid(): Boolean {
        return token.isNotBlank() && userId.isNotBlank() && userType.isNotBlank()
    }
}