// AuthBridgeService.kt
package com.example.gigwork.data.auth


import com.example.gigwork.domain.models.SupabaseUser
import com.example.gigwork.domain.models.SupabaseUserData
import com.example.gigwork.domain.models.UserType
import com.example.gigwork.domain.models.toSupabaseUser
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.lang.reflect.Array.set
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthBridgeService @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val preferenceManager: PreferenceManager
) {
    /**
     * Sign in with Firebase phone credential
     */
    suspend fun signInWithPhoneCredential(
        credential: PhoneAuthCredential
    ): Result<FirebaseUser> {
        return try {
            val authResult = Firebase.auth.signInWithCredential(credential).await()
            Result.success(authResult.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create or link a Supabase account using Firebase user information
     */
    suspend fun createOrLinkSupabaseAccount(
        firebaseUser: FirebaseUser,
        userType: UserType? = null
    ): Result<SupabaseUser> {
        if (firebaseUser.phoneNumber == null) {
            return Result.failure(Exception("Firebase user has no phone number"))
        }

        return try {
            // Check if user exists in Supabase by phone number
            val existingUser = findSupabaseUserByPhone(firebaseUser.phoneNumber!!)

            if (existingUser != null) {
                // User exists, update Firebase UID if needed and return
                if (existingUser.firebaseUid != firebaseUser.uid) {
                    updateFirebaseUid(existingUser.id, firebaseUser.uid)
                }

                // Sign in to Supabase using custom method (this requires server-side function)
                signInToSupabase(firebaseUser.phoneNumber!!, existingUser.id)

                // Save session data
                preferenceManager.saveUserIds(firebaseUser.uid, existingUser.id)
                preferenceManager.saveUserPhone(firebaseUser.phoneNumber!!)

                Result.success(existingUser)
            } else {
                // Create new Supabase user
                createSupabaseUser(firebaseUser.phoneNumber!!, firebaseUser.uid, userType)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a new Supabase user
     */
    private suspend fun createSupabaseUser(
        phoneNumber: String,
        firebaseUid: String,
        userType: UserType?
    ): Result<SupabaseUser> {
        return try {
            // Generate random password for Supabase (not used since auth is through Firebase)
            val password = UUID.randomUUID().toString()

            // Create JSON data for user metadata
            val userData = buildJsonObject {
                put("firebase_uid", firebaseUid)
                put("user_type", userType?.name ?: "")
            }

            // Directly create a user record in the database to bypass authentication issues
            val userUuid = UUID.randomUUID().toString()

            supabaseClient.postgrest["users"].insert(
                mapOf(
                    "id" to userUuid,
                    "phone" to phoneNumber,
                    "firebase_uid" to firebaseUid,
                    "user_type" to (userType?.name ?: ""),
                    "created_at" to Clock.System.now().toString()
                )
            )

            // Also create a profiles record if needed
            supabaseClient.postgrest["profiles"].insert(
                mapOf(
                    "id" to userUuid,
                    "phone" to phoneNumber,
                    "firebase_uid" to firebaseUid,
                    "user_type" to (userType?.name ?: "")
                )
            )

            val user = SupabaseUser(
                id = userUuid,
                phone = phoneNumber,
                firebaseUid = firebaseUid,
                userType = userType
            )

            // Save session data
            preferenceManager.saveUserIds(firebaseUid, user.id)
            preferenceManager.saveUserPhone(phoneNumber)
            userType?.let { preferenceManager.saveUserType(it.name) }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Find a Supabase user by phone number
     */
    private suspend fun findSupabaseUserByPhone(phoneNumber: String): SupabaseUser? {
        return try {
            val response = supabaseClient.postgrest["profiles"]
                .select {
                    filter {
                        eq("phone", phoneNumber)
                    }
                }
                .decodeSingleOrNull<SupabaseUserData>()

            response?.toSupabaseUser()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Update Firebase UID for a Supabase user
     */
    private suspend fun updateFirebaseUid(supabaseUid: String, firebaseUid: String) {
        try {
            supabaseClient.from("profiles")  // ✅ Use `.from("profiles")` instead of `.postgrest.from()`
                .update(mapOf("firebase_uid" to firebaseUid)) {
                    filter {
                        eq("id", supabaseUid)
                    }
                }


        } catch (e: Exception) {
            // Log but continue, this is not a critical operation
        }
    }

    /**
     * Sign in to Supabase (this requires a custom server-side function)
     */
    /**
     * Sign in to Supabase (this requires a custom server-side function)
     */
    private suspend fun signInToSupabase(phoneNumber: String, userId: String): Result<Unit> {
        return try {
            // This requires a server-side function or API that can generate a session
            // For demo purposes, we're using a direct database approach
            // In production, you should implement a secure token exchange

            // Check if session exists
            val session = supabaseClient.auth.currentSessionOrNull()

            if (session == null) {
                // Instead of trying to authenticate, just ensure the user record exists
                val userExists = supabaseClient.postgrest["profiles"]
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeList<Map<String, Any>>()
                    .isNotEmpty()

                if (!userExists) {
                    // Create user record if needed
                    supabaseClient.postgrest["profiles"].insert(
                        mapOf(
                            "id" to userId,
                            "phone" to phoneNumber
                        )
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Complete user profile with additional information
     */
    suspend fun completeUserProfile(
        fullName: String,
        email: String?,
        userType: UserType
    ): Result<SupabaseUser> {
        val userId = supabaseClient.auth.currentUserOrNull()?.id
            ?: return Result.failure(Exception("Not logged in to Supabase"))

        return try {
            // Update user data in profiles table
            val updateData = mutableMapOf<String, Any>(
                "full_name" to fullName,
                "user_type" to userType.name
            )

            // Add email if not null
            email?.let { updateData["email"] = it }

            // Update profile with the new format
            supabaseClient.from("profiles") //
                .update(updateData) {
                    filter { eq("id", userId) }  // Correct for Supabase 3.0+
                }

            // Update auth user with the new format
            val userData = buildJsonObject {
                put("full_name", fullName)
                put("user_type", userType.name)
            }

            // Updated auth.updateUser syntax
            supabaseClient.auth.updateUser {
                data = userData  // `this.data` should be used instead of setting email directly
                email?.let { this.email = it } //  Only update email if not null
            }
            // Save user type
            preferenceManager.saveUserType(userType.name)

            // Updated syntax for fetching the user
            val user = supabaseClient.from("profiles")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<SupabaseUserData>()
                ?.toSupabaseUser() ?: throw Exception("Failed to retrieve updated user profile")

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}