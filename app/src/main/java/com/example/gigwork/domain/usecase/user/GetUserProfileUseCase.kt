package com.example.gigwork.domain.usecase.user

import com.example.gigwork.core.error.ExceptionMapper
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.domain.models.UserType
import com.example.gigwork.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for retrieving user profiles.
 * Supports both retrieving by ID and retrieving the current user's profile.
 */
class GetUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    /**
     * Retrieves a user profile by ID.
     *
     * @param userId The ID of the user to retrieve
     * @return Flow of ApiResult containing the User if successful
     */
    suspend operator fun invoke(userId: String): Flow<ApiResult<User>> = flow {
        require(userId.isNotBlank()) { "User ID cannot be empty" }

        emit(ApiResult.Loading)

        try {
            withContext(dispatcher) {
                val userResultFlow = userRepository.getUser(userId)
                userResultFlow.collect { apiResult ->
                    // Using fold to handle the ApiResult states
                    apiResult.fold(
                        onSuccess = { user ->
                            emit(ApiResult.Success(user))
                        },
                        onError = { error ->
                            emit(ApiResult.Error(error))
                        },
                        onLoading = {
                            emit(ApiResult.Loading)
                        }
                    )
                }
            }
        } catch (e: Exception) {
            emit(
                ApiResult.Error(
                    AppError.UnexpectedError(
                        message = e.message ?: "Failed to get user profile",
                        cause = e
                    )
                )
            )
        }
    }

    /**
     * Retrieves the current user's profile.
     *
     * @return Flow of ApiResult containing the User if successful
     */
    suspend operator fun invoke(): Flow<ApiResult<User>> = flow {
        userRepository.getUserProfile()
            .catch { cause ->
                emit(ApiResult.Error(ExceptionMapper.map(cause, "PROFILE_FLOW_ERROR")))
            }
            .collect { result ->
                result.fold(
                    onSuccess = { profile ->
                        emit(ApiResult.Success(userProfileToUser(profile)))
                    },
                    onError = { error ->
                        emit(ApiResult.Error(error))
                    },
                    onLoading = {
                        emit(ApiResult.Loading)
                    }
                )
            }
    }.catch { cause ->
        emit(ApiResult.Error(ExceptionMapper.map(cause, "PROFILE_FLOW_ERROR")))
    }



    /**
     * Helper function to convert UserProfile to User
     */
    private fun userProfileToUser(profile: UserProfile): User {
        return User(
            id = profile.userId,
            name = profile.name,
            email = null, // Fill from profile if available
            phone = null, // Fill from profile if available
            type = determineUserType(profile),
            profile = profile
        )
    }

    /**
     * Determine user type based on profile data
     */
    private fun determineUserType(profile: UserProfile): UserType {
        // If profile has company-related fields filled, consider it an employer
        return if (profile.companyName != null || profile.companyFunction != null || profile.staffCount != null) {
            UserType.EMPLOYER
        } else {
            UserType.EMPLOYEE
        }
    }
}