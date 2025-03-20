// data/api/UserApi.kt
package com.example.gigwork.data.api

import com.example.gigwork.data.mappers.UserDto
import com.example.gigwork.data.mappers.UserProfileDto
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.gigwork.data.auth.AuthService
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import com.example.gigwork.util.Logger


interface UserApi {
    suspend fun getUser(userId: String): UserDto
    suspend fun updateProfile(profile: UserProfileDto)
    suspend fun deleteUser(userId: String)
    suspend fun createUser(userDto: UserDto): UserDto
    suspend fun createUserWithAuth(userDto: UserDto, password: String): UserDto
    suspend fun createProfile(profile: UserProfileDto)
    suspend fun getProfile(userId: String): UserProfileDto?
}

@Singleton
class UserApiImpl @Inject constructor(
    val supabaseClient: SupabaseClient,
    private val authService: AuthService,
    private val logger: Logger
) : UserApi {

    private val TAG = "UserApiImpl"

    override suspend fun createProfile(profile: UserProfileDto) {
        supabaseClient.client.postgrest["user_profiles"]
            .insert(profile)
    }

    // Add profile retrieval method
    override suspend fun getProfile(userId: String): UserProfileDto? {
        return try {
            supabaseClient.client.postgrest["user_profiles"]
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeSingle<UserProfileDto>()
        } catch (e: Exception) {
            null
        }
    }


    override suspend fun getUser(userId: String): UserDto {
        return try {
            // Handle empty responses with list decoding
            val users = supabaseClient.client.postgrest["users"]
                .select {
                    filter { eq("firebase_uid", userId) }
                }
                .decodeList<UserDto>()

            users.firstOrNull() ?: createNewUserInDatabase(userId)
        } catch (e: Exception) {
            logger.e(
                tag = "UserApiImpl",
                message = "Error getting user",
                throwable = e,
                additionalData = mapOf("firebase_uid" to userId)
            )
            createNewUserInDatabase(userId)
        }
    }

    override suspend fun createUser(userDto: UserDto): UserDto {
        return try {
            supabaseClient.client.postgrest["users"]
                .insert(userDto)
                .decodeSingle<UserDto>()
        } catch (e: Exception) {
            if (isDuplicateKeyError(e)) {
                // If user already exists, fetch it
                getUser(userDto.firebase_uid)
            } else {
                throw e
            }
        }
    }

    // Add this as a private helper method
    private suspend fun createNewUserInDatabase(firebaseUid: String): UserDto {
        return try {
            val firebaseUser = FirebaseAuth.getInstance().currentUser

            val newUser = UserDto(
                id = UUID.randomUUID().toString(),
                firebase_uid = firebaseUid,
                name = firebaseUser?.displayName ?: "New User",
                email = firebaseUser?.email,
                phone = firebaseUser?.phoneNumber,
                type = "EMPLOYEE"
            )

            createUser(newUser)
        } catch (e: Exception) {
            logger.e(
                tag = "UserApiImpl",
                message = "Error creating user, fetching existing",
                throwable = e
            )
            // Final fallback attempt to get user
            getUser(firebaseUid)
        }
    }

    override suspend fun createUserWithAuth(userDto: UserDto, password: String): UserDto {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting user registration with auth")

                // Validate that phone number exists
                if (userDto.phone.isNullOrBlank()) {
                    throw IllegalArgumentException("Phone number is required for registration")
                }

                // Use AuthService for registration with phone as mandatory
                val userId = authService.register(
                    phone = userDto.phone,
                    email = userDto.email,
                    password = password
                )

                Log.d(TAG, "Auth registration successful, got user ID: $userId")

                // Create user in database
                val finalUserDto = userDto.copy(id = userId)
                val createdUser = supabaseClient.client.postgrest["users"]
                    .insert(finalUserDto)
                    .decodeSingle<UserDto>()

                Log.d(TAG, "User record created in database, returning user")
                createdUser

            } catch (e: Exception) {
                Log.e(TAG, "Error during user registration: ${e.message}", e)
                throw Exception("User registration failed: ${e.message}", e)
            }
        }
    }

    override suspend fun updateProfile(profile: UserProfileDto) {
        try {
            supabaseClient.client.postgrest["user_profiles"]
                .update(profile) {
                    filter { eq("user_id", profile.userId) }
                }
        } catch (e: Exception) {
            // If update fails, try creating new profile
            if (e.message?.contains("404") == true) {
                createProfile(profile)
            } else {
                throw e
            }
        }
    }

    override suspend fun deleteUser(userId: String) {
        supabaseClient.client.postgrest["users"]
            .delete {
                filter {
                eq("id", userId)
            }}
    }

    private fun isDuplicateKeyError(e: Exception): Boolean {
        return e.message?.contains("duplicate key") == true
    }
}