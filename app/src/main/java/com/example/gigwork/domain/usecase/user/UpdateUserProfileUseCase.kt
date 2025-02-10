package com.example.gigwork.domain.usecase.user

import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.Result
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.data.repository.UserRepository
import com.example.gigwork.domain.usecase.base.FlowUseCase
import com.example.gigwork.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val logger: Logger,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<UpdateUserProfileUseCase.Params, Unit>(dispatcher) {

    data class Params(
        val profile: UserProfile
    ) {
        fun validate(): ValidationResult {
            val errors = mutableListOf<ValidationError>()

            if (profile.name.isBlank()) {
                errors.add(ValidationError("Name is required", "name"))
            }

            profile.email?.let { email ->
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    errors.add(ValidationError("Invalid email format", "email"))
                }
            }

            profile.phone?.let { phone ->
                if (!phone.matches(Regex("^\\+?[0-9]{10,13}$"))) {
                    errors.add(ValidationError("Invalid phone number format", "phone"))
                }
            }

            return if (errors.isEmpty()) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid(errors)
            }
        }
    }

    override suspend fun execute(parameters: Params): Flow<Result<Unit>> = flow {
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
                is ValidationResult.Valid -> {
                    emit(Result.Loading)
                    val startTime = System.currentTimeMillis()

                    userRepository.updateUserProfile(parameters.profile).collect { result ->
                        when (result) {
                            is Result.Success -> {
                                val duration = System.currentTimeMillis() - startTime
                                logger.i(
                                    tag = TAG,
                                    message = "Profile update successful",
                                    additionalData = mapOf(
                                        "user_id" to parameters.profile.userId,
                                        "duration_ms" to duration
                                    )
                                )
                                emit(Result.Success(Unit))
                            }
                            is Result.Error -> {
                                handleError(result.error, parameters)
                                emit(Result.Error(result.error))
                            }
                            is Result.Loading -> emit(Result.Loading)
                        }
                    }
                }
                is ValidationResult.Invalid -> {
                    logger.w(
                        tag = TAG,
                        message = "Profile validation failed",
                        additionalData = mapOf(
                            "user_id" to parameters.profile.userId,
                            "validation_errors" to validationResult.errors.joinToString()
                        )
                    )
                    emit(Result.Error(AppError.ValidationError(
                        message = validationResult.errors.joinToString { it.message },
                        field = validationResult.errors.firstOrNull()?.field
                    )))
                }
            }
        } catch (e: Exception) {
            handleUnexpectedError(e, parameters)
            emit(Result.Error(e.toAppError()))
        }
    }.catch { e ->
        handleUnexpectedError(e, parameters)
        emit(Result.Error(e.toAppError()))
    }

    private fun handleError(error: AppError, parameters: Params) {
        logger.e(
            tag = TAG,
            message = "Profile update failed",
            throwable = error,
            additionalData = mapOf(
                "user_id" to parameters.profile.userId,
                "error_type" to error.javaClass.simpleName
            )
        )
    }

    private fun handleUnexpectedError(error: Throwable, parameters: Params) {
        logger.e(
            tag = TAG,
            message = "Unexpected error during profile update",
            throwable = error,
            additionalData = mapOf(
                "user_id" to parameters.profile.userId,
                "error_type" to error.javaClass.simpleName
            )
        )
    }

    private fun getUpdatedFields(profile: UserProfile): Map<String, Any?> {
        return mapOf(
            "name" to profile.name,
            "email" to profile.email,
            "phone" to profile.phone,
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

    private data class ValidationError(
        val message: String,
        val field: String
    )

    private sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val errors: List<ValidationError>) : ValidationResult()
    }

    companion object {
        private const val TAG = "UpdateUserProfileUseCase"
    }
}