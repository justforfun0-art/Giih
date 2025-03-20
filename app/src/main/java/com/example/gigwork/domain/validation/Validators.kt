package com.example.gigwork.domain.validation

import android.util.Patterns
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.models.Location
import com.example.gigwork.domain.models.UserProfile
import java.util.regex.Pattern

/**
 * Centralized validation logic for the application.
 * Provides type-safe validation for various input types.
 */
object Validators {

    // Constants for validation rules
    private const val MIN_TITLE_LENGTH = 3
    private const val MIN_DESCRIPTION_LENGTH = 10
    private const val MIN_NAME_LENGTH = 2
    private const val PHONE_LENGTH = 10
    private const val MIN_SALARY = 0.0
    private const val MAX_SALARY = 1000000.0
    private const val MIN_DURATION = 1
    private const val MAX_DURATION = 365

    val ALLOWED_SALARY_UNITS = setOf("hourly", "daily", "weekly", "monthly")
    val ALLOWED_DURATION_UNITS = setOf("hours", "days", "weeks", "months")
    private val ALLOWED_JOB_STATUSES = setOf("OPEN", "CLOSED", "PENDING", "DELETED", "ACTIVE")

    /**
     * Validates complete job input
     */
    fun validateJob(job: Job): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // Basic validations
        validateJobTitle(job.title)?.let { errors.add(it) }
        validateJobDescription(job.description)?.let { errors.add(it) }
        validateSalary(job.salary, job.salaryUnit)?.let { errors.add(it) }
        validateWorkDuration(job.workDuration, job.workDurationUnit)?.let { errors.add(it) }
        validateLocation(job.location)?.let { errors.add(it) }
        validateStatus(job.status)?.let { errors.add(it) }

        // Employer validation
        if (job.employerId.isBlank()) {
            errors.add(ValidationError("Employer ID is required", "employerId"))
        }

        return if (errors.isEmpty()) ValidationResult.Success else ValidationResult.Error(errors)
    }

    /**
     * Validates job title
     */
    fun validateJobTitle(title: String): ValidationError? {
        return when {
            title.isBlank() -> ValidationError("Title is required", "title")
            title.length < MIN_TITLE_LENGTH -> ValidationError(
                "Title must be at least $MIN_TITLE_LENGTH characters",
                "title"
            )
            title.length > 100 -> ValidationError(
                "Title must not exceed 100 characters",
                "title"
            )
            else -> null
        }
    }

    /**
     * Validates job description
     */
    fun validateJobDescription(description: String): ValidationError? {
        return when {
            description.isBlank() -> ValidationError("Description is required", "description")
            description.length < MIN_DESCRIPTION_LENGTH -> ValidationError(
                "Description must be at least $MIN_DESCRIPTION_LENGTH characters",
                "description"
            )
            description.length > 5000 -> ValidationError(
                "Description must not exceed 5000 characters",
                "description"
            )
            else -> null
        }
    }

    /**
     * Validates salary and salary unit
     */
    fun validateSalary(salary: Double, salaryUnit: String): ValidationError? {
        return when {
            salary <= MIN_SALARY -> ValidationError(
                "Salary must be greater than $MIN_SALARY",
                "salary"
            )
            salary > MAX_SALARY -> ValidationError(
                "Salary must not exceed $MAX_SALARY",
                "salary"
            )
            !ALLOWED_SALARY_UNITS.contains(salaryUnit.lowercase()) -> ValidationError(
                "Invalid salary unit. Allowed values: ${ALLOWED_SALARY_UNITS.joinToString(", ")}",
                "salaryUnit"
            )
            else -> null
        }
    }

    /**
     * Validates work duration and unit
     */
    fun validateWorkDuration(duration: Int, durationUnit: String): ValidationError? {
        return when {
            duration < MIN_DURATION -> ValidationError(
                "Duration must be at least $MIN_DURATION",
                "workDuration"
            )
            duration > MAX_DURATION -> ValidationError(
                "Duration must not exceed $MAX_DURATION",
                "workDuration"
            )
            !ALLOWED_DURATION_UNITS.contains(durationUnit.lowercase()) -> ValidationError(
                "Invalid duration unit. Allowed values: ${ALLOWED_DURATION_UNITS.joinToString(", ")}",
                "workDurationUnit"
            )
            else -> null
        }
    }

    /**
     * Validates job status
     */
    fun validateStatus(status: String): ValidationError? {
        return if (!ALLOWED_JOB_STATUSES.contains(status.uppercase())) {
            ValidationError(
                "Invalid status. Allowed values: ${ALLOWED_JOB_STATUSES.joinToString(", ")}",
                "status"
            )
        } else null
    }

    /**
     * Validates location
     */
    fun validateLocation(location: Location): ValidationError? {
        return when {
            location.state.isBlank() -> ValidationError("State is required", "location.state")
            location.district.isBlank() -> ValidationError("District is required", "location.district")
            location.latitude != null && (location.latitude < -90 || location.latitude > 90) ->
                ValidationError("Invalid latitude", "location.latitude")
            location.longitude != null && (location.longitude < -180 || location.longitude > 180) ->
                ValidationError("Invalid longitude", "location.longitude")
            else -> null
        }
    }

    /**
     * Validates user profile input
     */
    fun validateUserProfile(profile: UserProfile): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // Validate name
        if (profile.name.length < MIN_NAME_LENGTH) {
            errors.add(ValidationError("Name must be at least $MIN_NAME_LENGTH characters", "name"))
        }

        // Validate email if provided
        profile.dateOfBirth?.let {
            if (!Patterns.EMAIL_ADDRESS.matcher(it).matches()) {
                errors.add(ValidationError("Invalid email format", "email"))
            }
        }

        // Validate phone if provided
        profile.photo?.let {
            if (!Pattern.matches("^[0-9]{$PHONE_LENGTH}$", it)) {
                errors.add(ValidationError("Phone number must be $PHONE_LENGTH digits", "phone"))
            }
        }

        // Validate location
        profile.currentLocation?.let {
            validateLocation(it)?.let { error -> errors.add(error) }
        }

        // Validate preferred location if provided
        profile.preferredLocation?.let {
            validateLocation(it)?.let { error -> errors.add(error) }
        }

        return if (errors.isEmpty()) ValidationResult.Success else ValidationResult.Error(errors)
    }

    /**
     * Validates search query
     */
    fun validateSearchQuery(query: String): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        if (query.isNotBlank() && query.length < 2) {
            errors.add(ValidationError("Search query must be at least 2 characters", "query"))
        }

        return if (errors.isEmpty()) ValidationResult.Success else ValidationResult.Error(errors)
    }

    /**
     * Job validation using the generic validation rules
     */
    object JobValidation {
        val titleRules = listOf(
            StringValidationRule.NotEmpty("Job title is required"),
            StringValidationRule.MinLength(3, "Job title must be at least 3 characters"),
            StringValidationRule.MaxLength(100, "Job title cannot exceed 100 characters")
        )

        val descriptionRules = listOf(
            StringValidationRule.NotEmpty("Job description is required"),
            StringValidationRule.MinLength(20, "Job description must be at least 20 characters")
        )

        val salaryRules = listOf(
            NumberValidationRule.Minimum(0.0, "Salary must be greater than 0"),
            NumberValidationRule.Maximum(1000000.0, "Salary cannot exceed 1,000,000")
        )

        // Custom validation
        val locationValidator = CustomValidationRule<Location>(
            validationFunction = { location ->
                location.state.isNotBlank() && location.district.isNotBlank()
            },
            errorMessage = "Both state and district are required"
        )

        /**
         * Example of using the generic validation rules
         */
        fun validateJobWithRules(job: Job): ValidationResult {
            // Validate title
            job.title.validate(*titleRules.toTypedArray()).let {
                if (it is ValidationResult.SingleError) return it.copy(field = "title")
            }

            // Validate description
            job.description.validate(*descriptionRules.toTypedArray()).let {
                if (it is ValidationResult.SingleError) return it.copy(field = "description")
            }

            // Validate salary
            job.salary.validate(*salaryRules.toTypedArray()).let {
                if (it is ValidationResult.SingleError) return it.copy(field = "salary")
            }

            // Validate location
            locationValidator.validate(job.location).let {
                if (it is ValidationResult.SingleError) return it.copy(field = "location")
            }

            return ValidationResult.Success
        }
    }
}