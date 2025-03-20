package com.example.gigwork.domain.usecase

import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ValidateCredentialsUseCase @Inject constructor() {
    data class Params(
        val identifier: String,
        val password: String
    )

    suspend operator fun invoke(params: Params): ApiResult<Boolean> {
        // Validate identifier (email or phone)
        if (params.identifier.isBlank()) {
            return ApiResult.Error(
                AppError.ValidationError(
                    message = "Email or phone number is required",
                    field = "identifier"
                )
            )
        }

        // Validate password
        if (params.password.isBlank()) {
            return ApiResult.Error(
                AppError.ValidationError(
                    message = "Password is required",
                    field = "password"
                )
            )
        }

        // Simple email validation
        if (params.identifier.contains("@")) {
            val emailRegex = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
            if (!params.identifier.matches(Regex(emailRegex))) {
                return ApiResult.Error(
                    AppError.ValidationError(
                        message = "Invalid email format",
                        field = "identifier"
                    )
                )
            }
        }
        // Phone number validation
        else if (params.identifier.all { it.isDigit() }) {
            if (params.identifier.length < 10) {
                return ApiResult.Error(
                    AppError.ValidationError(
                        message = "Phone number should be at least 10 digits",
                        field = "identifier"
                    )
                )
            }
        } else {
            return ApiResult.Error(
                AppError.ValidationError(
                    message = "Please enter a valid email or phone number",
                    field = "identifier"
                )
            )
        }

        // Password strength validation
        if (params.password.length < 8) {
            return ApiResult.Error(
                AppError.ValidationError(
                    message = "Password should be at least 8 characters",
                    field = "password"
                )
            )
        }

        return ApiResult.Success(true)
    }
}