package com.example.gigwork.domain.usecase

import android.util.Patterns
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Use case for validating registration form fields individually.
 */
class ValidateRegistrationFieldsUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    companion object {
        private const val MIN_NAME_LENGTH = 2
        private const val MAX_NAME_LENGTH = 50
        private const val MIN_PASSWORD_LENGTH = 6
        private const val MAX_PASSWORD_LENGTH = 100
        private const val MIN_PHONE_LENGTH = 10
        private const val MAX_PHONE_LENGTH = 15
    }

    /**
     * Validates a field based on its name and value.
     *
     * @param field The name of the field to validate
     * @param value The value to validate
     * @return Flow of ApiResult indicating validation success or error
     */
    fun validateField(field: String, value: String): Flow<ApiResult<Unit>> = flow {
        val result = when (field.lowercase()) {
            "name" -> validateName(value)
            "email" -> validateEmail(value)
            "phone" -> validatePhone(value)
            "password" -> validatePassword(value)
            else -> ApiResult.Success(Unit) // Unknown fields are considered valid
        }

        emit(result)
    }.flowOn(dispatcher)

    /**
     * Validates the name field.
     */
    private fun validateName(name: String): ApiResult<Unit> {
        return when {
            name.isBlank() -> ApiResult.Error(
                AppError.ValidationError("Name is required", "name")
            )
            name.length < MIN_NAME_LENGTH -> ApiResult.Error(
                AppError.ValidationError("Name is too short", "name")
            )
            name.length > MAX_NAME_LENGTH -> ApiResult.Error(
                AppError.ValidationError("Name is too long", "name")
            )
            else -> ApiResult.Success(Unit)
        }
    }

    /**
     * Validates the email field.
     */
    private fun validateEmail(email: String): ApiResult<Unit> {
        if (email.isBlank()) {
            return ApiResult.Success(Unit) // Email can be blank if phone is provided
        }

        return when {
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> ApiResult.Error(
                AppError.ValidationError("Invalid email format", "email")
            )
            else -> ApiResult.Success(Unit)
        }
    }

    /**
     * Validates the phone field.
     */
    private fun validatePhone(phone: String): ApiResult<Unit> {
        if (phone.isBlank()) {
            return ApiResult.Success(Unit) // Phone can be blank if email is provided
        }

        return when {
            !phone.all { it.isDigit() || it == '+' || it == '-' || it == ' ' } -> ApiResult.Error(
                AppError.ValidationError("Phone number can only contain digits, spaces, plus or minus signs", "phone")
            )
            phone.count { it.isDigit() } < MIN_PHONE_LENGTH -> ApiResult.Error(
                AppError.ValidationError("Phone number is too short", "phone")
            )
            phone.count { it.isDigit() } > MAX_PHONE_LENGTH -> ApiResult.Error(
                AppError.ValidationError("Phone number is too long", "phone")
            )
            else -> ApiResult.Success(Unit)
        }
    }

    /**
     * Validates the password field.
     */
    private fun validatePassword(password: String): ApiResult<Unit> {
        return when {
            password.isBlank() -> ApiResult.Error(
                AppError.ValidationError("Password is required", "password")
            )
            password.length < MIN_PASSWORD_LENGTH -> ApiResult.Error(
                AppError.ValidationError("Password must be at least $MIN_PASSWORD_LENGTH characters long", "password")
            )
            password.length > MAX_PASSWORD_LENGTH -> ApiResult.Error(
                AppError.ValidationError("Password is too long", "password")
            )
            !password.any { it.isDigit() } -> ApiResult.Error(
                AppError.ValidationError("Password must contain at least one digit", "password")
            )
            !password.any { it.isLetter() } -> ApiResult.Error(
                AppError.ValidationError("Password must contain at least one letter", "password")
            )
            else -> ApiResult.Success(Unit)
        }
    }
}