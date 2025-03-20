package com.example.gigwork.data.security

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedPreferences constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "EncryptedPreferences"
        private const val PREFS_FILENAME = "secure_prefs"
        // Add these constants at the top of the Companion object
        private const val KEY_TEMP_PASSWORD = "temp_password"
        private const val KEY_TEMP_PASSWORD_TIMESTAMP = "temp_password_timestamp"
        private const val TEMP_PASSWORD_EXPIRY = 5 * 60 * 1000 // 5 minutes in milliseconds

        // Keys for stored values
        private const val KEY_SUPABASE_URL = "supabase_url"
        private const val KEY_SUPABASE_KEY = "supabase_key"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }

    private val masterKeyAlias by lazy {
        try {
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating master key", e)
            throw SecurityException("Failed to create master key", e)
        }
    }
    fun saveTemporaryPassword(password: String) {
        try {
            saveString(KEY_TEMP_PASSWORD, password)
            saveString(KEY_TEMP_PASSWORD_TIMESTAMP, System.currentTimeMillis().toString())
            Log.d(TAG, "Temporary password saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving temporary password", e)
        }
    }

    fun getTemporaryPassword(): String {
        try {
            // Check if the temporary password has expired
            val timestamp = getString(KEY_TEMP_PASSWORD_TIMESTAMP, "0").toLongOrNull() ?: 0L
            val currentTime = System.currentTimeMillis()

            return if (currentTime - timestamp > TEMP_PASSWORD_EXPIRY) {
                // Password has expired, clear it and return empty string
                Log.d(TAG, "Temporary password has expired")
                clearTemporaryPassword()
                ""
            } else {
                // Return the password
                val password = getString(KEY_TEMP_PASSWORD, "")
                Log.d(TAG, "Retrieved temporary password successfully")
                password
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving temporary password", e)
            return ""
        }
    }

    fun clearTemporaryPassword() {
        try {
            removeKey(KEY_TEMP_PASSWORD)
            removeKey(KEY_TEMP_PASSWORD_TIMESTAMP)
            Log.d(TAG, "Temporary password cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing temporary password", e)
        }
    }
    private val prefs by lazy {
        try {
            EncryptedSharedPreferences.create(
                PREFS_FILENAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating EncryptedSharedPreferences", e)
            throw SecurityException("Failed to create encrypted preferences", e)
        }
    }

    fun saveString(key: String, value: String) {
        try {
            prefs.edit().putString(key, value).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving encrypted value", e)
            throw SecurityException("Failed to save encrypted value", e)
        }
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return try {
            prefs.getString(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            Log.e(TAG, "Error reading encrypted value", e)
            defaultValue
        }
    }

    fun removeKey(key: String) {
        try {
            prefs.edit().remove(key).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error removing key", e)
            throw SecurityException("Failed to remove key", e)
        }
    }

    fun clearAll() {
        try {
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing preferences", e)
            throw SecurityException("Failed to clear preferences", e)
        }
    }

    // Convenience methods for common operations
    fun saveSupabaseCredentials(url: String, key: String) {
        saveString(KEY_SUPABASE_URL, url)
        saveString(KEY_SUPABASE_KEY, key)
    }

    fun getSupabaseUrl(): String = getString(KEY_SUPABASE_URL)
    fun getSupabaseKey(): String = getString(KEY_SUPABASE_KEY)

    fun saveAuthToken(token: String) = saveString(KEY_AUTH_TOKEN, token)
    fun getAuthToken(): String = getString(KEY_AUTH_TOKEN)

    fun saveUserId(userId: String) = saveString(KEY_USER_ID, userId)
    fun getUserId(): String = getString(KEY_USER_ID)

    fun saveRefreshToken(token: String) = saveString(KEY_REFRESH_TOKEN, token)
    fun getRefreshToken(): String = getString(KEY_REFRESH_TOKEN)

    fun clearAuthData() {
        removeKey(KEY_AUTH_TOKEN)
        removeKey(KEY_USER_ID)
        removeKey(KEY_REFRESH_TOKEN)
    }

    fun hasValidAuth(): Boolean {
        return getAuthToken().isNotBlank() && getUserId().isNotBlank()
    }
}