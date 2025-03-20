// ConfigProvider.kt
package com.example.gigwork.data.config

interface ConfigProvider {
    fun getSupabaseUrl(): String
    fun getSupabaseKey(): String
    fun getDatabaseName(): String
    fun getPreferencesName(): String
    fun getNetworkTimeout(): Long
    fun getDefaultPageSize(): Int
}