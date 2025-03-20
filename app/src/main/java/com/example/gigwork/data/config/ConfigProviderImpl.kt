// app/src/main/java/com/example/gigwork/data/config/ConfigProviderImpl.kt
package com.example.gigwork.data.config

import android.content.Context
import com.example.gigwork.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ConfigProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ConfigProvider {
    override fun getSupabaseUrl(): String = BuildConfig.SUPABASE_URL

    override fun getSupabaseKey(): String = BuildConfig.SUPABASE_KEY

    override fun getDatabaseName(): String = "gigwork_database"

    override fun getPreferencesName(): String = "gigwork_preferences"

    override fun getNetworkTimeout(): Long = 30_000L // 30 seconds

    override fun getDefaultPageSize(): Int = 20
}