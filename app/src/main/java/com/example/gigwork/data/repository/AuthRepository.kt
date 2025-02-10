// File: app/src/main/java/com/example/gigwork/data/repository/AuthRepository.kt
package com.example.gigwork.data.repository

import com.example.gigwork.data.security.EncryptedPreferences
import com.example.gigwork.domain.models.AuthState
import com.example.gigwork.domain.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val encryptedPreferences: EncryptedPreferences
) {
    fun saveAuthData(token: String, userId: String, userType: String) {
        encryptedPreferences.apply {
            saveAuthToken(token)
            saveUserId(userId)
            saveString("user_type", userType)
        }
    }

    fun getAuthData(): AuthState {
        return AuthState(
            token = encryptedPreferences.getAuthToken(),
            userId = encryptedPreferences.getUserId(),
            userType = encryptedPreferences.getString("user_type")
        )
    }

    fun clearAuthData() {
        encryptedPreferences.clearAuthData()
    }

    fun isUserLoggedIn(): Boolean {
        return encryptedPreferences.hasValidAuth()
    }

    fun saveUserPreferences(
        language: String,
        notificationsEnabled: Boolean,
        darkMode: Boolean
    ) {
        encryptedPreferences.apply {
            saveString("pref_language", language)
            saveString("pref_notifications", notificationsEnabled.toString())
            saveString("pref_dark_mode", darkMode.toString())
        }
    }

    fun getUserPreferences(): Map<String, String> {
        return mapOf(
            "language" to encryptedPreferences.getString("pref_language", "en"),
            "notifications" to encryptedPreferences.getString("pref_notifications", "true"),
            "darkMode" to encryptedPreferences.getString("pref_dark_mode", "false")
        )
    }
}
