package com.example.gigwork.data.security

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "EncryptedPreferences"
        private const val PREFS_FILENAME = "secure_prefs"

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