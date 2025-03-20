package com.example.gigwork.data.repository

import com.example.gigwork.core.error.ExceptionMapper
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.data.api.UserApi
import com.example.gigwork.data.database.*
import com.example.gigwork.data.mappers.toDto
import com.example.gigwork.data.mappers.toDomain
import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import java.text.SimpleDateFormat
import java.util.Locale

@Singleton
class UserRepositoryLegacy @Inject constructor(
    private val api: UserApi,
    private val userDao: UserDao,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "UserRepository"
        private const val CACHE_DURATION = 30 * 60 * 1000L // 30 minutes
    }

    suspend fun getUser(userId: String): Flow<ApiResult<User>> = flow {
        emit(ApiResult.Loading)

        try {
            // Check cache first
            userDao.getUser(userId)?.let { cachedUser ->
                val timestamp = userDao.getUserTimestamp(userId) ?: 0L
                if (!isCacheExpired(timestamp)) {
                    logger.d(
                        tag = TAG,
                        message = "Returning cached user data",
                        additionalData = mapOf(
                            "user_id" to userId,
                            "cache_age" to (System.currentTimeMillis() - timestamp)
                        )
                    )
                    emit(ApiResult.Success(cachedUser.toDomain()))
                    return@flow
                }
            }

            // Fetch from network
            logger.d(
                tag = TAG,
                message = "Fetching user from network",
                additionalData = mapOf("user_id" to userId)
            )

            val networkUser = api.getUser(userId)
            val domainUser = networkUser.toDomain()

            // Cache the result
            userDao.insertUser(domainUser.toEntity())
            domainUser.profile?.let { profile ->
                userDao.insertUserProfile(profile.toEntity())
            }

            logger.i(
                tag = TAG,
                message = "Successfully fetched and cached user data",
                additionalData = mapOf(
                    "user_id" to userId,
                    "has_profile" to (domainUser.profile != null),
                    "cache_timestamp" to System.currentTimeMillis()
                )
            )

            emit(ApiResult.Success(domainUser))
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Failed to get user data",
                throwable = e,
                additionalData = mapOf(
                    "user_id" to userId,
                    "error_type" to e.javaClass.simpleName,
                    "has_cache" to (userDao.getUser(userId) != null)
                )
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_USER")))
        }
    }

    suspend fun updateUserProfile(profile: UserProfile): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)

        try {
            val validationErrors = validateProfile(profile)
            if (validationErrors.isNotEmpty()) {
                throw ValidationException(validationErrors)
            }

            logger.d(
                tag = TAG,
                message = "Updating user profile",
                additionalData = mapOf(
                    "user_id" to profile.userId,
                    "update_fields" to getUpdatedFields(profile)
                )
            )

            val startTime = System.currentTimeMillis()
            api.updateProfile(profile.toDto())
            val duration = System.currentTimeMillis() - startTime

            // Update cache
            userDao.updateUserProfile(profile.toEntity())

            logger.i(
                tag = TAG,
                message = "Successfully updated user profile",
                additionalData = mapOf(
                    "user_id" to profile.userId,
                    "duration_ms" to duration,
                    "cache_updated" to true
                )
            )

            emit(ApiResult.Success(Unit))
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Failed to update user profile",
                throwable = e,
                additionalData = mapOf(
                    "user_id" to profile.userId,
                    "error_type" to e.javaClass.simpleName,
                    "validation_errors" to validateProfile(profile)
                )
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "UPDATE_PROFILE")))
        }
    }

    private fun isCacheExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > CACHE_DURATION
    }

    private fun validateProfile(profile: UserProfile): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (profile.name.isBlank()) {
            errors["name"] = "Name cannot be empty"
        }

        if (profile.userId.isBlank()) {
            errors["userId"] = "User ID cannot be empty"
        }

        if (profile.dateOfBirth != null && !isValidDate(profile.dateOfBirth)) {
            errors["dateOfBirth"] = "Invalid date format"
        }

        return errors
    }

    private fun isValidDate(dateStr: String): Boolean {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            format.isLenient = false
            format.parse(dateStr)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getUpdatedFields(profile: UserProfile): Map<String, Any?> {
        return mapOf(
            "name" to profile.name,
            "photo" to profile.photo,
            "dateOfBirth" to profile.dateOfBirth,
            "gender" to profile.gender,
            "currentLocation" to profile.currentLocation,
            "preferredLocation" to profile.preferredLocation,
            "qualification" to profile.qualification,
            "computerKnowledge" to profile.computerKnowledge,
            "aadharNumber" to profile.aadharNumber,
            "companyName" to profile.companyName,
            "companyFunction" to profile.companyFunction,
            "staffCount" to profile.staffCount,
            "yearlyTurnover" to profile.yearlyTurnover
        ).filterValues { it != null }
    }

    class ValidationException(val errors: Map<String, String>) : Exception("Validation failed")
}