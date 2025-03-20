package com.example.gigwork.data.repository

import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.core.result.toAppError
import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.data.mappers.UserDto
import com.example.gigwork.data.mappers.UserProfileDto
import com.example.gigwork.data.mappers.toDto
import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.domain.repository.UserRepository
import com.example.gigwork.util.Logger
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import com.example.gigwork.data.mappers.toDomain

@Singleton
class SupabaseUserRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val logger: Logger
) : UserRepository {

    companion object {
        private const val TAG = "SupabaseUserRepository"
    }

    override suspend fun userExists(userId: String): Flow<ApiResult<Boolean>> = flow {
        emit(ApiResult.Loading)

        try {
            val results = supabaseClient.client.postgrest
                .from("users")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeList<Map<String, Any>>()

            emit(ApiResult.Success(results.isNotEmpty()))
        } catch (e: Exception) {
            logger.e("SupabaseUserRepository", "Error checking if user exists", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }

    override suspend fun getUser(userId: String): Flow<ApiResult<User>> = flow {
        emit(ApiResult.Loading)

        try {
            // Fetch user
            val userResults = supabaseClient.client.postgrest
                .from("users")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeList<UserDto>()

            if (userResults.isEmpty()) {
                throw Exception("User not found")
            }

            val user = userResults.first()

            // Fetch profile in a separate call
            val profileResults = supabaseClient.client.postgrest
                .from("user_profiles")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<UserProfileDto>()

            val profile = if (profileResults.isNotEmpty()) profileResults.first() else null
            val userWithProfile = user.copy(profile = profile)

            // Use fully qualified name for the mapper function
            emit(
                ApiResult.Success<User>(
                    userWithProfile.toDomain()
                )
            )

        } catch (e: Exception) {
            logger.e(TAG, "Error fetching user", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }

    override suspend fun createUser(user: User): Flow<ApiResult<User>> = flow {
        emit(ApiResult.Loading)

        try {
            // Insert user
            val userDto = user.toDto()
            supabaseClient.client.postgrest
                .from("users")
                .insert(userDto)

            // Insert profile if exists
            user.profile?.let { profile ->
                val profileDto = profile.toDto()
                supabaseClient.client.postgrest
                    .from("user_profiles")
                    .insert(profileDto)
            }

            emit(ApiResult.Success(user))
        } catch (e: Exception) {
            logger.e(TAG, "Error creating user", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }

    override suspend fun updateUserProfile(profile: UserProfile): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)

        try {
            val profileDto = profile.toDto()

            // Check if profile exists
            val exists = supabaseClient.client.postgrest
                .from("user_profiles")
                .select {
                    filter {
                        eq("user_id", profile.userId)
                    }
                }
                .decodeList<Map<String, Any>>()
                .isNotEmpty()

            if (exists) {
                // Update
                val map = profileDto.toMap()  // ✅ Convert DTO to a map

                supabaseClient.client.postgrest
                    .from("user_profiles")
                    .update(map) {  // ✅ Pass the entire map directly
                        filter { eq("user_id", profile.userId) }
                    }

            } else {
                // Insert
                supabaseClient.client.postgrest
                    .from("user_profiles")
                    .insert(profileDto)
            }

            emit(ApiResult.Success(Unit))
        } catch (e: Exception) {
            logger.e(TAG, "Error updating user profile", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }

    override suspend fun deleteUser(userId: String): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)

        try {
            // Delete profile first (to maintain referential integrity)
            supabaseClient.client.postgrest
                .from("user_profiles")
                .delete {
                    filter {
                        eq("user_id", userId)
                    }
                }

            supabaseClient.client.postgrest
                .from("users")
                .delete {
                    filter {
                        eq("id", userId)
                    }
                }

            emit(ApiResult.Success(Unit))
        } catch (e: Exception) {
            logger.e(TAG, "Error deleting user", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }

    override suspend fun searchUsers(query: String): Flow<ApiResult<List<User>>> = flow {
        emit(ApiResult.Loading)

        try {
            // Implementation for searching users
            // For Supabase 3.0, we can use OR conditions
            val results = supabaseClient.client.postgrest
                .from("users")
                .select {
                    filter {
                        or {
                            ilike("name", "%$query%")
                            ilike("email", "%$query%")
                        }
                    }
                }
                .decodeList<UserDto>()

            // Convert to domain users EXPLICITLY with no mapper functions
            val users = results.map { dto ->
                com.example.gigwork.domain.models.User(
                    id = dto.id,
                    name = dto.name,
                    email = dto.email,
                    phone = dto.phone,
                    type = com.example.gigwork.domain.models.UserType.valueOf(dto.type),
                    profile = dto.profile?.let { profileDto ->
                        com.example.gigwork.domain.models.UserProfile(
                            id = profileDto.id,
                            userId = profileDto.userId,
                            name = profileDto.name,
                            photo = profileDto.profilePhoto,
                            dateOfBirth = profileDto.dateOfBirth,
                            gender = profileDto.gender,
                            currentLocation = profileDto.currentLocation?.let { locDto ->
                                com.example.gigwork.domain.models.Location(
                                    latitude = locDto.latitude,
                                    longitude = locDto.longitude,
                                    address = locDto.address,
                                    pinCode = locDto.pinCode,
                                    state = locDto.state ?: "",
                                    district = locDto.district ?: ""
                                )
                            },
                            preferredLocation = profileDto.preferredLocation?.let { locDto ->
                                com.example.gigwork.domain.models.Location(
                                    latitude = locDto.latitude,
                                    longitude = locDto.longitude,
                                    address = locDto.address,
                                    pinCode = locDto.pinCode,
                                    state = locDto.state ?: "",
                                    district = locDto.district ?: ""
                                )
                            },
                            qualification = profileDto.qualification,
                            computerKnowledge = profileDto.computerKnowledge,
                            aadharNumber = profileDto.aadharNumber,
                            companyName = profileDto.companyName,
                            companyFunction = profileDto.companyFunction,
                            staffCount = profileDto.staffCount,
                            yearlyTurnover = profileDto.yearlyTurnover
                        )
                    }
                )
            }

            // This should now return the correct type
            emit(ApiResult.Success(users))
        } catch (e: Exception) {
            logger.e(TAG, "Error searching users", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }

    override suspend fun getUserByEmail(email: String): Flow<ApiResult<User>> = flow {
        emit(ApiResult.Loading)

        try {
            val results = supabaseClient.client.postgrest
                .from("users")
                .select {
                    filter {
                        eq("email", email)
                    }
                }
                .decodeList<UserDto>()

            if (results.isEmpty()) {
                throw Exception("User not found with email: $email")
            }

            val user = results.first()
            emit(ApiResult.Success(user.toDomain()))
        } catch (e: Exception) {
            logger.e(TAG, "Error getting user by email", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }

    override suspend fun updateUserStatus(userId: String, status: String): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)

        try {
            supabaseClient.client.postgrest
                .from("users")
                .update(mapOf("status" to status)) {  // ✅ Pass map directly
                    filter { eq("id", userId) }  // ✅ Apply filter inside update block
                }


            emit(ApiResult.Success(Unit))
        } catch (e: Exception) {
            logger.e(TAG, "Error updating user status", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }

    override suspend fun getUsersByRole(role: String): Flow<ApiResult<List<User>>> = flow {
        emit(ApiResult.Loading)

        try {
            val results = supabaseClient.client.postgrest
                .from("users")
                .select {
                    filter {
                        eq("role", role)
                    }
                }
                .decodeList<UserDto>()

            val users = results.map { it.toDomain() }
            emit(ApiResult.Success(users))
        } catch (e: Exception) {
            logger.e(TAG, "Error getting users by role", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }

    override suspend fun getUserProfile(): Flow<ApiResult<UserProfile>> = flow {
        emit(ApiResult.Loading)

        try {
            val userId = getCurrentUserId()
            if (userId.isBlank()) {
                throw Exception("No logged in user")
            }

            val results = supabaseClient.client.postgrest
                .from("user_profiles")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<UserProfileDto>()

            if (results.isEmpty()) {
                throw Exception("User profile not found")
            }

            val profile = results.first().toDomain()
            emit(ApiResult.Success(profile))
        } catch (e: Exception) {
            logger.e(TAG, "Error getting user profile", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }

    override suspend fun getCurrentUserId(): String {
        return supabaseClient.client.auth.currentUserOrNull()?.id ?: ""
    }

    override suspend fun getUsersByIds(userIds: List<String>): Flow<ApiResult<Map<String, User>>> = flow {
        emit(ApiResult.Loading)

        try {
            if (userIds.isEmpty()) {
                emit(ApiResult.Success(emptyMap()))
                return@flow
            }

            // For Supabase 3.0, we can use the IN operator
            val userMap = mutableMapOf<String, User>()

            for (userId in userIds) {
                try {
                    val results = supabaseClient.client.postgrest
                        .from("users")
                        .select {
                            filter {
                                eq("id", userId)
                            }
                        }
                        .decodeList<UserDto>()

                    if (results.isNotEmpty()) {
                        val user = results.first().toDomain()
                        userMap[userId] = user
                    }
                } catch (e: Exception) {
                    // Log but continue, we want to get as many users as possible
                    logger.e(TAG, "Error fetching user $userId", e)
                }
            }

            emit(ApiResult.Success(userMap))
        } catch (e: Exception) {
            logger.e(TAG, "Error getting users by ids", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }

    // Helper extension function to convert DTO to Map
    private fun UserProfileDto.toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "user_id" to userId,
            "name" to name,
            "photo" to profilePhoto,
            "date_of_birth" to dateOfBirth,
            "gender" to gender,
            "qualification" to qualification,
            "computer_knowledge" to computerKnowledge,
            "aadhar_number" to aadharNumber,
            "company_name" to companyName,
            "company_function" to companyFunction,
            "staff_count" to staffCount,
            "yearly_turnover" to yearlyTurnover,
            // Map other fields as needed
        )
    }
}