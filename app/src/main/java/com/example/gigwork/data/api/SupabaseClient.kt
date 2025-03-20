package com.example.gigwork.data.api

import com.example.gigwork.data.config.ConfigProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.storage.Storage  // Add this after adding the dependency
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseClient @Inject constructor(
    private val configProvider: ConfigProvider
) {
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = configProvider.getSupabaseUrl(),
        supabaseKey = configProvider.getSupabaseKey()
    ) {
        // Use install() method for Supabase 3.0
        install(Postgrest)
        install(Auth)
        install(Storage)  // Add this after adding the dependency
    }
}