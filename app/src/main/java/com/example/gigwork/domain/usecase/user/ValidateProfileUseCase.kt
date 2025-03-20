package com.example.gigwork.domain.usecase.user

import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.UserProfile
import javax.inject.Inject

/**
 * Use case for validating user profile fields
 */
class ValidateProfileUseCase @Inject constructor() {

    /**
     * Validates the entire user profile
     * @param profile UserProfile to validate
     * @return ApiResult indicating success or validation error
     */
    operator fun invoke(profile: UserProfile): ApiResult<Unit> {
        val errors = mutableListOf<ValidationError>()

        // Validate essential fields
        if (profile.name.isBlank()) {
            errors.add(ValidationError("Name is required", "name"))
        }

        profile.dateOfBirth?.let { dob ->
            if (dob.isBlank()) {
                errors.add(ValidationError("Date of birth is required", "dateOfBirth"))
            }
            // Additional date validation can be added here
        } ?: errors.add(ValidationError("Date of birth is required", "dateOfBirth"))

        profile.gender?.let { gender ->
            if (gender.isBlank()) {
                errors.add(ValidationError("Gender is required", "gender"))
            }
        } ?: errors.add(ValidationError("Gender is required", "gender"))

        profile.qualification?.let { qualification ->
            if (qualification.isBlank()) {
                errors.add(ValidationError("Qualification is required", "qualification"))
            }
        } ?: errors.add(ValidationError("Qualification is required", "qualification"))

        // Location validations
        profile.currentLocation?.let { location ->
            if (location.state.isBlank() || location.district.isBlank()) {
                errors.add(ValidationError("Complete location information is required", "currentLocation"))
            }
        } ?: errors.add(ValidationError("Current location is required", "currentLocation"))

        return if (errors.isEmpty()) {
            ApiResult.Success(Unit)
        } else {
            ApiResult.Error(
                AppError.ValidationError(
                    message = errors.joinToString("; ") { it.message },
                    field = errors.firstOrNull()?.field
                )
            )
        }
    }

    /**
     * Validates a single field of the user profile
     * @param field The name of the field to validate
     * @param value The value to validate
     * @return ApiResult indicating success or validation error
     */
    fun validateField(field: String, value: String): ApiResult<Unit> {
        return when (field) {
            "name" -> validateName(value)
            "dateOfBirth" -> validateDateOfBirth(value)
            "gender" -> validateGender(value)
            "qualification" -> validateQualification(value)
            "phone" -> validatePhone(value)
            "email" -> validateEmail(value)
            else -> ApiResult.Success(Unit) // Fields without specific validation rules pass by default
        }
    }

    /**
     * Validates employer-specific fields
     * @param field The name of the field to validate
     * @param value The value to validate
     * @return ApiResult indicating success or validation error
     */
    fun validateEmployerField(field: String, value: String): ApiResult<Unit> {
        return when (field) {
            "companyName" -> validateCompanyName(value)
            "companyFunction" -> validateCompanyFunction(value)
            "staffCount" -> validateStaffCount(value)
            "yearlyTurnover" -> validateYearlyTurnover(value)
            else -> validateField(field, value) // Delegate to general validation for common fields
        }
    }

    /**
     * Validates an employer profile
     * @param profile UserProfile to validate
     * @return ApiResult indicating success or validation error
     */
    fun validateEmployerProfile(profile: UserProfile): ApiResult<Unit> {
        val errors = mutableListOf<ValidationError>()

        // Basic validations
        val basicValidation = invoke(profile)
        if (basicValidation is ApiResult.Error) {
            return basicValidation
        }

        // Employer-specific validations
        profile.companyName?.let { companyName ->
            if (companyName.isBlank()) {
                errors.add(ValidationError("Company name is required", "companyName"))
            }
        } ?: errors.add(ValidationError("Company name is required", "companyName"))

        profile.companyFunction?.let { companyFunction ->
            if (companyFunction.isBlank()) {
                errors.add(ValidationError("Company function is required", "companyFunction"))
            }
        }

        profile.staffCount?.let { staffCount ->
            if (staffCount < 0) {
                errors.add(ValidationError("Staff count cannot be negative", "staffCount"))
            }
        }

        return if (errors.isEmpty()) {
            ApiResult.Success(Unit)
        } else {
            ApiResult.Error(
                AppError.ValidationError(
                    message = errors.joinToString("; ") { it.message },
                    field = errors.firstOrNull()?.field
                )
            )
        }
    }

    // Individual field validation methods

    private fun validateName(name: String): ApiResult<Unit> {
        return if (name.isBlank()) {
            ApiResult.Error(AppError.ValidationError("Name is required", "name"))
        } else if (name.length < 2) {
            ApiResult.Error(AppError.ValidationError("Name is too short", "name"))
        } else if (name.length > 50) {
            ApiResult.Error(AppError.ValidationError("Name is too long", "name"))
        } else {
            ApiResult.Success(Unit)
        }
    }

    private fun validateDateOfBirth(dob: String): ApiResult<Unit> {
        return if (dob.isBlank()) {
            ApiResult.Error(AppError.ValidationError("Date of birth is required", "dateOfBirth"))
        } else {
            // Additional date validation logic can be added here
            ApiResult.Success(Unit)
        }
    }

    private fun validateGender(gender: String): ApiResult<Unit> {
        return if (gender.isBlank()) {
            ApiResult.Error(AppError.ValidationError("Gender is required", "gender"))
        } else {
            ApiResult.Success(Unit)
        }
    }

    private fun validateQualification(qualification: String): ApiResult<Unit> {
        return if (qualification.isBlank()) {
            ApiResult.Error(AppError.ValidationError("Qualification is required", "qualification"))
        } else {
            ApiResult.Success(Unit)
        }
    }

    private fun validatePhone(phone: String): ApiResult<Unit> {
        // Simple phone validation - can be enhanced with regex patterns
        return if (phone.isBlank()) {
            ApiResult.Success(Unit) // Phone can be optional
        } else if (phone.length < 10) {
            ApiResult.Error(AppError.ValidationError("Phone number is too short", "phone"))
        } else {
            ApiResult.Success(Unit)
        }
    }

    private fun validateEmail(email: String): ApiResult<Unit> {
        // Simple email validation - can be enhanced with regex patterns
        return if (email.isBlank()) {
            ApiResult.Success(Unit) // Email can be optional
        } else if (!email.contains("@") || !email.contains(".")) {
            ApiResult.Error(AppError.ValidationError("Invalid email format", "email"))
        } else {
            ApiResult.Success(Unit)
        }
    }

    private fun validateCompanyName(companyName: String): ApiResult<Unit> {
        return if (companyName.isBlank()) {
            ApiResult.Error(AppError.ValidationError("Company name is required", "companyName"))
        } else if (companyName.length < 2) {
            ApiResult.Error(AppError.ValidationError("Company name is too short", "companyName"))
        } else {
            ApiResult.Success(Unit)
        }
    }

    private fun validateCompanyFunction(companyFunction: String): ApiResult<Unit> {
        return if (companyFunction.isBlank()) {
            ApiResult.Error(AppError.ValidationError("Company function is required", "companyFunction"))
        } else {
            ApiResult.Success(Unit)
        }
    }

    private fun validateStaffCount(staffCount: String): ApiResult<Unit> {
        return try {
            val count = staffCount.toInt()
            if (count < 0) {
                ApiResult.Error(AppError.ValidationError("Staff count cannot be negative", "staffCount"))
            } else {
                ApiResult.Success(Unit)
            }
        } catch (e: NumberFormatException) {
            ApiResult.Error(AppError.ValidationError("Staff count must be a number", "staffCount"))
        }
    }

    private fun validateYearlyTurnover(yearlyTurnover: String): ApiResult<Unit> {
        return if (yearlyTurnover.isBlank()) {
            ApiResult.Success(Unit) // Turnover can be optional
        } else {
            ApiResult.Success(Unit)
        }
    }

    data class ValidationError(
        val message: String,
        val field: String
    )
}