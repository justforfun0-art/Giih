package com.example.gigwork.data.config

import com.example.gigwork.data.security.EncryptedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfig @Inject constructor(
    private val encryptedPreferences: EncryptedPreferences
) : ConfigProvider {
    companion object {
        private const val KEY_SUPABASE_URL = "supabase_url"
        private const val KEY_SUPABASE_KEY = "supabase_key"

        // Application constants
        const val DATABASE_NAME = "gigwork_database"
        const val PREFERENCES_NAME = "gigwork_preferences"
        const val NETWORK_TIMEOUT = 30000L
        const val DEFAULT_PAGE_SIZE = 20

        // TODO: Replace these with BuildConfig values once Gradle generation is set up
        private const val SUPABASE_URL = "your_supabase_url_here"
        private const val SUPABASE_KEY = "your_supabase_key_here"
    }

    init {
        if (encryptedPreferences.getString(KEY_SUPABASE_URL).isEmpty()) {
            encryptedPreferences.saveString(KEY_SUPABASE_URL, SUPABASE_URL)
        }
        if (encryptedPreferences.getString(KEY_SUPABASE_KEY).isEmpty()) {
            encryptedPreferences.saveString(KEY_SUPABASE_KEY, SUPABASE_KEY)
        }
    }

    override fun getSupabaseUrl(): String = encryptedPreferences.getString(KEY_SUPABASE_URL)
    override fun getSupabaseKey(): String = encryptedPreferences.getString(KEY_SUPABASE_KEY)
    override fun getDatabaseName(): String = DATABASE_NAME
    override fun getPreferencesName(): String = PREFERENCES_NAME
    override fun getNetworkTimeout(): Long = NETWORK_TIMEOUT
    override fun getDefaultPageSize(): Int = DEFAULT_PAGE_SIZE
}