// data/api/SupabaseClient.kt
package com.example.gigwork.data.api

import com.example.gigwork.data.config.ConfigProvider
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.gotrue.GoTrue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseClient @Inject constructor(
    private val configProvider: ConfigProvider
) {
    val client = createSupabaseClient(
        supabaseUrl = configProvider.getSupabaseUrl(),
        supabaseKey = configProvider.getSupabaseKey()
    ) {
        install(Postgrest)
        install(GoTrue)
    }
}