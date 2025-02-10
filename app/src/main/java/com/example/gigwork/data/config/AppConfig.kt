// data/config/AppConfig.kt
package com.example.gigwork.data.config

import com.example.gigwork.data.security.EncryptedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfig @Inject constructor(
    private val encryptedPreferences: EncryptedPreferences
) {
    companion object {
        private const val KEY_SUPABASE_URL = "supabase_url"
        private const val KEY_SUPABASE_KEY = "supabase_key"
    }

    var supabaseUrl: String
        get() = encryptedPreferences.getString(KEY_SUPABASE_URL)
        set(value) = encryptedPreferences.saveString(KEY_SUPABASE_URL, value)

    var supabaseKey: String
        get() = encryptedPreferences.getString(KEY_SUPABASE_KEY)
        set(value) = encryptedPreferences.saveString(KEY_SUPABASE_KEY, value)
}
