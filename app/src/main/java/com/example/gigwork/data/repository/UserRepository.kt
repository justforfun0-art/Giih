package com.example.gigwork.data.repository

import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.data.api.UserApi
import com.example.gigwork.data.db.UserDao
import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val api: UserApi,
    private val userDao: UserDao,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "UserRepository"
        private const val CACHE_DURATION = 30 * 60 * 1000L // 30 minutes
    }

    suspend fun getUser(userId: String): Flow<Result<User>> = flow {
        emit(Result.Loading)

        try {
            // Check cache first
            val cachedUser = userDao.getUser(userId)
            if (cachedUser != null && !isCacheExpired(cachedUser.timestamp)) {
                logger.d(
                    tag = TAG,
                    message = "Returning cached user data",
                    additionalData = mapOf(
                        "user_id" to userId,
                        "cache_age" to (System.currentTimeMillis() - cachedUser.timestamp)
                    )
                )
                emit(Result.Success(cachedUser.toDomain()))
                return@flow
            }

            // Fetch from network
            logger.d(
                tag = TAG,
                message = "Fetching user from network",
                additionalData = mapOf("user_id" to userId)
            )

            val networkUser = api.getUser(userId)

            // Cache the result
            userDao.insertUser(networkUser.toEntity())

            logger.i(
                tag = TAG,
                message = "Successfully fetched and cached user data",
                additionalData = mapOf(
                    "user_id" to userId,
                    "has_profile" to (networkUser.profile != null),
                    "cache_timestamp" to System.currentTimeMillis()
                )
            )

            emit(Result.Success(networkUser.toDomain()))
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
            emit(Result.Error(e.toAppError()))
        }
    }

    suspend fun updateUserProfile(profile: UserProfile): Flow<Result<Unit>> = flow {
        emit(Result.Loading)

        try {
            logger.d(
                tag = TAG,
                message = "Updating user profile",
                additionalData = mapOf(
                    "user_id" to profile.userId,
                    "update_fields" to profile.getUpdatedFields()
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

            emit(Result.Success(Unit))
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Failed to update user profile",
                throwable = e,
                additionalData = mapOf(
                    "user_id" to profile.userId,
                    "error_type" to e.javaClass.simpleName,
                    "validation_errors" to getValidationErrors(profile)
                )
            )
            emit(Result.Error(e.toAppError()))
        }
    }.catch { e ->
        logger.e(
            tag = TAG,
            message = "Unexpected error in updateUserProfile flow",
            throwable = e,
            additionalData = mapOf(
                "error_type" to e.javaClass.simpleName
            )
        )
        emit(Result.Error(e.toAppError()))
    }

    private fun isCacheExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > CACHE_DURATION
    }

    private fun getValidationErrors(profile: UserProfile): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (profile.name.isBlank()) errors["name"] = "Name cannot be empty"
        if (profile.email?.isNotBlank() == true && !profile.email.isValidEmail()) {
            errors["email"] = "Invalid email format"
        }
        return errors
    }
}