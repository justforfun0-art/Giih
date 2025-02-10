// data/config/ConfigProvider.kt
package com.example.gigwork.data.config

import javax.inject.Inject
import javax.inject.Singleton

interface ConfigProvider {
    fun getSupabaseUrl(): String
    fun getSupabaseKey(): String
}

@Singleton
class ProductionConfigProvider @Inject constructor() : ConfigProvider {
    override fun getSupabaseUrl(): String = BuildConfig.SUPABASE_URL

    override fun getSupabaseKey(): String = BuildConfig.SUPABASE_KEY
}