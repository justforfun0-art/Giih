// HybridSessionManager.kt
package com.example.gigwork.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HybridSessionManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val preferenceManager: PreferenceManager
) {
    private val auth: FirebaseAuth get() = Firebase.auth

    // Check if the user is authenticated in both Firebase & Supabase
    fun isAuthenticated(): Flow<Boolean> {
        val firebaseAuth = flow { emit(auth.currentUser != null) }
        val supabaseAuth = flow {
            val user = try {
                supabaseClient.auth.retrieveUserForCurrentSession()
            } catch (e: Exception) {
                null
            }
            emit(user != null)
        }

        return firebaseAuth.combine(supabaseAuth) { firebase, supabase ->
            firebase && supabase
        }
    }

    fun getFirebaseUser() = auth.currentUser

    suspend fun getSupabaseUser(): UserInfo? {
        return try {
            supabaseClient.auth.retrieveUserForCurrentSession()
        } catch (e: Exception) {
            null
        }
    }

    // Sign out from both systems
    suspend fun signOut() {
        // Sign out from Firebase
        auth.signOut()

        // Sign out from Supabase
        try {
            supabaseClient.auth.signOut()
        } catch (e: Exception) {
            // Handle exception, but continue to clear local data
        }

        // Clear any stored session data
        preferenceManager.clearSessionData()
    }

    // Get current user ID (prioritize Supabase ID for database operations)
    suspend fun getCurrentUserId(): String? {
        return try {
            supabaseClient.auth.retrieveUserForCurrentSession()?.id
        } catch (e: Exception) {
            auth.currentUser?.uid
        }
    }
}
