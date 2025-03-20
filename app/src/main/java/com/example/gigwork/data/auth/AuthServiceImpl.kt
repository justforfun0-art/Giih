// path: com/example/gigwork/data/auth/AuthServiceImpl.kt
package com.example.gigwork.data.auth

import android.app.Activity
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.domain.models.AuthState
import com.example.gigwork.util.Logger
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import io.github.jan.supabase.postgrest.postgrest// Replace these problematic imports:
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import java.util.concurrent.TimeUnit
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthServiceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val supabaseClient: SupabaseClient,
    private val preferenceManager: PreferenceManager,
    private val logger: Logger
) : AuthService {
    companion object {
        private const val TAG = "AuthServiceImpl"
    }

    override suspend fun isUserLoggedIn(): Boolean {
        return preferenceManager.hasValidAuth() && auth.currentUser != null
    }

    override suspend fun getAuthData(): AuthState {
        return AuthState(
            token = preferenceManager.getAuthToken(),
            userId = preferenceManager.getUserId(),
            userType = preferenceManager.getString("user_type")
        )
    }

    override suspend fun clearAuthData() {
        preferenceManager.clearAuthData()
        auth.signOut()
    }

    override suspend fun saveAuthData(token: String, userId: String, userType: String) {
        preferenceManager.saveAuthToken(token)
        preferenceManager.saveUserId(userId)
        preferenceManager.saveString("user_type", userType)
    }

    override fun sendVerificationCode(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        logger.d(TAG, "Sending verification code to $phoneNumber")

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    override fun signInWithPhoneAuthCredential(
        credential: PhoneAuthCredential
    ): Flow<ApiResult<String>> = flow {
        emit(ApiResult.Loading)

        try {
            val result = Tasks.await(auth.signInWithCredential(credential))
            val user = result.user

            if (user != null) {
                // Save Firebase UID
                val userId = user.uid
                preferenceManager.saveUserId(userId)

                // Check if user exists in Supabase
                val userExists = checkUserExistsInSupabase(userId)

                if (!userExists) {
                    // Create placeholder user in Supabase
                    createPlaceholderUserInSupabase(userId, user.phoneNumber ?: "")
                }

                emit(ApiResult.Success(userId))
            } else {
                emit(ApiResult.Error(AppError.SecurityError(
                    message = "Authentication failed: User is null",
                    securityDomain = "authentication"
                )))
            }
        } catch (e: Exception) {
            logger.e(TAG, "Sign in with phone failed", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }

    // Implementation for Supabase 2.2.0
    override suspend fun register(
        phone: String,
        email: String?,
        password: String
    ): String {
        logger.d(TAG, "Registering user with phone: $phone, email: $email")

        try {
            // Generate a unique ID for the user
            val userId = UUID.randomUUID().toString()

            // Create a user record directly in the database
            // This bypasses the auth API completely
            supabaseClient.client.postgrest["users"]
                .insert(mapOf(
                    "id" to userId,
                    "name" to "New User",
                    "email" to (email ?: ""),
                    "phone" to phone,
                    "type" to "EMPLOYEE",
                    "created_at" to Clock.System.now().toString()
                ))

            logger.d(TAG, "Created user record in Supabase database")

            // Save user info locally
            preferenceManager.saveUserId(userId)
            preferenceManager.saveUserPhone(phone)

            return userId
        } catch (e: Exception) {
            logger.e(TAG, "Registration failed: ${e.message}", e)
            throw Exception("Failed to register user: ${e.message}", e)
        }
    }

    private suspend fun linkFirebaseToSupabase(firebaseUid: String, supabaseUid: String) {
        try {
            // Update Supabase user profile with Firebase UID

                    supabaseClient.client.postgrest["profiles"]
                        .update(
                            value = mapOf("firebase_uid" to firebaseUid)
                        ) {filter {
                            eq("id", supabaseUid)  // ✅ Use `.eq()` directly instead of `FilterOperator.EQ`
                        }}

        } catch (e: Exception) {
            // Log but continue, this is not a critical operation
            logger.e(TAG, "Error linking Firebase to Supabase", e)
        }
    }

    private suspend fun checkUserExistsInSupabase(userId: String): Boolean {
        return try {
            // Use the enumeration for the filter operator instead of a string

            val results = supabaseClient.client.postgrest.from("users")
                .select {
                    filter {
                        eq("id", userId)  // ✅ Use `.eq()` directly
                    }
                }
                .decodeList<Map<String, Any>>()

            results.isNotEmpty()
        } catch (e: Exception) {
            logger.e(TAG, "Error checking if user exists in Supabase", e)
            false
        }
    }

    private suspend fun createPlaceholderUserInSupabase(userId: String, phoneNumber: String) {
        try {
            // Using Map instead of DSL to avoid issues with the set function
            val userData = mapOf(
                "id" to userId,
                "phone_number" to phoneNumber,
                "created_at" to Clock.System.now().toString()
            )

            supabaseClient.client.postgrest["users"].insert(userData)

            logger.d(TAG, "Created placeholder user in Supabase", mapOf(
                "user_id" to userId,
                "phone_number" to phoneNumber
            ))
        } catch (e: Exception) {
            logger.e(TAG, "Error creating placeholder user in Supabase", e)
            throw e
        }
    }

    override suspend fun getUserType(userId: String): Flow<ApiResult<String>> = flow {
        emit(ApiResult.Loading)

        try {
            // First check locally
            val cachedUserType = preferenceManager.getString("user_type")
            if (cachedUserType.isNotBlank()) {
                emit(ApiResult.Success(cachedUserType))
                return@flow
            }

            // Try to get from Supabase
            try {

                val results = supabaseClient.client.postgrest.from("users")
                    .select {
                        filter {
                        eq("id", userId)  // ✅ Correct way to apply filtering
                    }}

                    .decodeList<Map<String, Any>>()

                // Extract user type from response
                val userType = if (results.isNotEmpty()) {
                    (results.first()["user_type"] as? String) ?: ""
                } else {
                    ""
                }

                preferenceManager.saveString("user_type", userType)
                emit(ApiResult.Success(userType))
            } catch (e: Exception) {
                logger.e(TAG, "Error getting user type from Supabase", e)
                emit(ApiResult.Success("")) // Return empty string if not found
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error getting user type", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }

    override suspend fun updateUserType(userId: String, userType: String): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)

        try {
            // Update in Supabase

                    supabaseClient.client.postgrest["users"]
                        .update(
                            value = mapOf("user_type" to userType)
                        ) {
                            filter {
                            eq("id", userId)  // ✅ Correct way to apply filtering in Supabase 3.0
                        }}


            // Update local storage
            preferenceManager.saveString("user_type", userType)

            emit(ApiResult.Success(Unit))
        } catch (e: Exception) {
            logger.e(TAG, "Error updating user type", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }

    override fun getUserPreferences(): Map<String, String> {
        // Use runBlocking for non-suspend function calling suspend function
        return runBlocking {
            mapOf(
                "language" to preferenceManager.getString("pref_language", "en"),
                "notifications" to preferenceManager.getString("pref_notifications", "true"),
                "darkMode" to preferenceManager.getString("pref_dark_mode", "false")
            )
        }
    }

    // Helper extension to convert Exception to AppError
    private fun Exception.toAppError(): AppError {
        return when (this) {
            is AppError -> this
            else -> AppError.UnexpectedError(
                message = this.message ?: "An unexpected error occurred",
                cause = this
            )
        }
    }
}