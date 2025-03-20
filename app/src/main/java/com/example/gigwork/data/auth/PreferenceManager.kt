// PreferenceManager.kt
package com.example.gigwork.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class PreferenceManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private val FIREBASE_UID = stringPreferencesKey("firebase_uid")
        private val SUPABASE_UID = stringPreferencesKey("supabase_uid")
        private val USER_PHONE = stringPreferencesKey("user_phone")
        private val USER_TYPE = stringPreferencesKey("user_type")
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val USER_ID = stringPreferencesKey("user_id")
        private val TOKEN_EXPIRY = longPreferencesKey("token_expiry")
    }

    // Save methods
    suspend fun saveUserIds(firebaseUid: String, supabaseUid: String) {
        context.dataStore.edit { preferences ->
            preferences[FIREBASE_UID] = firebaseUid
            preferences[SUPABASE_UID] = supabaseUid
        }
    }

    suspend fun saveUserPhone(phone: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_PHONE] = phone
        }
    }

    suspend fun saveUserType(userType: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_TYPE] = userType
        }
    }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = token
            // Set token expiry to 1 hour from now
            preferences[TOKEN_EXPIRY] = System.currentTimeMillis() + (60 * 60 * 1000)
        }
    }

    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = userId
        }
    }

    suspend fun saveString(key: String, value: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }

    // Get methods
    val firebaseUid: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[FIREBASE_UID] }

    val supabaseUid: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[SUPABASE_UID] }

    val userPhone: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[USER_PHONE] }

    val userType: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[USER_TYPE] }

    suspend fun getAuthToken(): String {
        return context.dataStore.data
            .map { preferences -> preferences[AUTH_TOKEN] ?: "" }
            .first()
    }

    suspend fun getUserId(): String {
        return context.dataStore.data
            .map { preferences -> preferences[USER_ID] ?: "" }
            .first()
    }

    suspend fun getString(key: String, defaultValue: String = ""): String {
        return context.dataStore.data
            .map { preferences -> preferences[stringPreferencesKey(key)] ?: defaultValue }
            .first()
    }

    suspend fun hasValidAuth(): Boolean {
        val token = getAuthToken()
        if (token.isBlank()) return false

        // Check token expiry
        val expiry = context.dataStore.data
            .map { preferences -> preferences[TOKEN_EXPIRY] ?: 0L }
            .first()

        return token.isNotBlank() && expiry > System.currentTimeMillis()
    }

    // Clear methods
    suspend fun clearSessionData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun clearAuthData() {
        context.dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN)
            preferences.remove(TOKEN_EXPIRY)
            preferences.remove(USER_ID)
        }
    }
}