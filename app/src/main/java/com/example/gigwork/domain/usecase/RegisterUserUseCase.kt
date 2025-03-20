package com.example.gigwork.domain.usecase

import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.models.UserType
import com.example.gigwork.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Use case for registering a new user in the system.
 */
class RegisterUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    /**
     * Data class containing parameters for user registration.
     */
    data class Params(
        val name: String,
        val email: String? = null,
        val phone: String? = null,
        val password: String,
        val isEmployer: Boolean = false
    )

    /**
     * Creates a new user in the system.
     *
     * @param params The registration parameters
     * @return Flow of ApiResult containing the created User on success or an error
     */
    suspend operator fun invoke(params: Params): Flow<ApiResult<User>> = flow {
        // Input validation
        if (params.name.isBlank()) {
            emit(ApiResult.Error(AppError.ValidationError("Name is required", field = "name")))
            return@flow
        }

        if (params.email.isNullOrBlank() && params.phone.isNullOrBlank()) {
            emit(ApiResult.Error(AppError.ValidationError(
                "Either email or phone is required",
                field = "contact"
            )))
            return@flow
        }

        if (params.password.isBlank()) {
            emit(ApiResult.Error(AppError.ValidationError("Password is required", field = "password")))
            return@flow
        }

        if (params.password.length < 6) {
            emit(ApiResult.Error(AppError.ValidationError(
                "Password must be at least 6 characters long",
                field = "password"
            )))
            return@flow
        }

        emit(ApiResult.Loading)

        try {
            // Create a user object with the provided information
            val user = User(
                id = "", // Will be assigned by the backend
                name = params.name,
                email = params.email,
                phone = params.phone,
                type = if (params.isEmployer) UserType.EMPLOYER else UserType.EMPLOYEE,
                profile = null // Profile will be created separately
            )

            // Create the user in the repository
            userRepository.createUser(user).collect { result ->
                result.fold(
                    onSuccess = { createdUser ->
                        emit(ApiResult.Success(createdUser))
                    },
                    onError = { error ->
                        emit(ApiResult.Error(error))
                    },
                    onLoading = {
                        // This state should not occur but handle it anyway
                        emit(ApiResult.Loading)
                    }
                )
            }
        } catch (e: Exception) {
            val error = when (e) {
                is AppError -> e
                else -> AppError.UnexpectedError(
                    message = e.message ?: "Failed to register user",
                    cause = e
                )
            }
            emit(ApiResult.Error(error))
        }
    }.flowOn(dispatcher)
}