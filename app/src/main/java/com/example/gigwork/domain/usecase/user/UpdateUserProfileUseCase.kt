package com.example.gigwork.domain.usecase.user

import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.error.model.NavigationError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.domain.repository.UserRepository
import com.example.gigwork.domain.usecase.base.FlowUseCase
import com.example.gigwork.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import java.sql.SQLException
import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val logger: Logger,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<UpdateUserProfileUseCase.Params, Unit>(dispatcher) {

    data class Params(
        val profile: UserProfile
    ) {
        fun validate(): ApiResult<Unit> {
            val errors = mutableListOf<ValidationError>()

            // Basic validation
            if (profile.name.isBlank()) {
                errors.add(ValidationError("Name is required", "name"))
            }

            // Employer-specific validations
            if (profile.companyName?.isBlank() == true) {
                errors.add(ValidationError("Company name cannot be empty if provided", "company_name"))
            }

            if (profile.staffCount != null && profile.staffCount < 0) {
                errors.add(ValidationError("Staff count cannot be negative", "staff_count"))
            }

            // Location validations
            // Location validations
            profile.currentLocation?.let { location ->
                val isValidLatitude = location.latitude == null || location.latitude in -90.0..90.0
                val isValidLongitude = location.longitude == null || location.longitude in -180.0..180.0
                if (!isValidLatitude || !isValidLongitude) {
                    errors.add(ValidationError("Invalid location coordinates", "current_location"))
                }
            }

            profile.preferredLocation?.let { location ->
                val isValidLatitude = location.latitude == null || location.latitude in -90.0..90.0
                val isValidLongitude = location.longitude == null || location.longitude in -180.0..180.0
                if (!isValidLatitude || !isValidLongitude) {
                    errors.add(ValidationError("Invalid location coordinates", "preferred_location"))
                }
            }

            return if (errors.isEmpty()) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(AppError.ValidationError(
                    message = errors.joinToString("; ") { it.message },
                    field = errors.firstOrNull()?.field
                ))
            }
        }
    }
    override suspend fun execute(parameters: Params): Flow<Unit> = flow {
        try {
            logger.d(
                tag = TAG,
                message = "Starting profile update",
                additionalData = mapOf(
                    "user_id" to parameters.profile.userId,
                    "fields_updated" to getUpdatedFields(parameters.profile)
                )
            )

            when (val validationResult = parameters.validate()) {
                is ApiResult.Success<Unit> -> {
                    val startTime = System.currentTimeMillis()

                    userRepository.updateUserProfile(parameters.profile).collect { apiResult ->
                        // Convert Result<Unit> to ApiResult<Unit>
                        apiResult.fold(
                            onSuccess = {
                                val duration = System.currentTimeMillis() - startTime
                                logger.i(
                                    tag = TAG,
                                    message = "Profile update successful",
                                    additionalData = mapOf(
                                        "user_id" to parameters.profile.userId,
                                        "duration_ms" to duration
                                    )
                                )
                                emit(Unit)
                            },
                            onError = { error ->
                                logger.e(
                                    tag = TAG,
                                    message = "Profile update failed",
                                    throwable = error.cause,
                                    additionalData = mapOf("user_id" to parameters.profile.userId)
                                )
                                throw error.toThrowable()
                            },
                            onLoading = {
                                logger.d(
                                    tag = TAG,
                                    message = "Profile update in progress",
                                    additionalData = mapOf("user_id" to parameters.profile.userId)
                                )
                            }

                        )
                    }
                }
                is ApiResult.Error -> {
                    logger.w(
                        tag = TAG,
                        message = "Profile validation failed",
                        additionalData = mapOf(
                            "user_id" to parameters.profile.userId,
                            "validation_errors" to validationResult.error.message
                        )
                    )
                    throw validationResult.error.toThrowable()
                }
                else -> {
                    // Loading state not applicable here
                }
            }
        } catch (e: Throwable) {
            logger.e(
                tag = TAG,
                message = "Error during profile update",
                throwable = e,
                additionalData = mapOf(
                    "user_id" to parameters.profile.userId,
                    "error_type" to e::class.simpleName
                )
            )
            throw e
        }
    }
    private fun getUpdatedFields(profile: UserProfile): Map<String, Any?> {
        return mapOf(
            "name" to profile.name,
            "date_of_birth" to profile.dateOfBirth,
            "gender" to profile.gender,
            "current_location" to profile.currentLocation,
            "preferred_location" to profile.preferredLocation,
            "qualification" to profile.qualification,
            "computer_knowledge" to profile.computerKnowledge,
            "photo" to profile.photo,
            "company_name" to profile.companyName,
            "company_function" to profile.companyFunction,
            "staff_count" to profile.staffCount,
            "yearly_turnover" to profile.yearlyTurnover
        ).filterValues { it != null }
    }

    private fun AppError.toThrowable(): Throwable = when (this) {
        is AppError.ValidationError -> IllegalArgumentException(message)
        is AppError.NetworkError -> IOException(message, cause)
        is AppError.DatabaseError -> SQLException(message, cause)
        is AppError.SecurityError -> SecurityException(message, cause)
        is AppError.FileError -> IOException(message, cause)
        is AppError.CacheError -> IOException(message, cause)
        is AppError.BusinessError -> IllegalStateException(message)
        is AppError.UnexpectedError -> Exception(message, cause)

        // Handle NavigationError subtypes
        is NavigationError.UnauthorizedNavigation -> SecurityException(message)
        is NavigationError.InvalidDeepLink -> IllegalArgumentException(message, cause)
        is NavigationError.NavigationFailed -> RuntimeException(message, cause)
        is NavigationError.InvalidRoute -> IllegalArgumentException(message)

        // If new error types are added in the future, this else branch will handle them
        // This ensures the when expression is always exhaustive
        else -> Exception(message, cause)
    }

    private data class ValidationError(
        val message: String,
        val field: String
    )

    companion object {
        private const val TAG = "UpdateUserProfileUseCase"
    }
}