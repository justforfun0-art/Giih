package com.example.gigwork.domain.validation

/**
 * Base class for validation rules
 */
sealed class ValidationRule<T> {
    abstract fun validate(value: T): ValidationResult
    abstract val errorMessage: String
}

/**
 * Result of a validation operation
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(
        val message: String,
        val field: String? = null,
        val errorCode: String? = null
    ) : ValidationResult()

    fun isValid() = this is Success
}

/**
 * String validation rules
 */
sealed class StringValidationRule : ValidationRule<String>() {

    data class NotEmpty(
        override val errorMessage: String = "Field cannot be empty"
    ) : StringValidationRule() {
        override fun validate(value: String): ValidationResult {
            return if (value.isNotBlank()) {
                ValidationResult.Success
            } else {
                ValidationResult.Error(errorMessage)
            }
        }
    }

    data class MinLength(
        val length: Int,
        override val errorMessage: String = "Minimum length is $length characters"
    ) : StringValidationRule() {
        override fun validate(value: String): ValidationResult {
            return if (value.length >= length) {
                ValidationResult.Success
            } else {
                ValidationResult.Error(errorMessage)
            }
        }
    }

    data class MaxLength(
        val length: Int,
        override val errorMessage: String = "Maximum length is $length characters"
    ) : StringValidationRule() {
        override fun validate(value: String): ValidationResult {
            return if (value.length <= length) {
                ValidationResult.Success
            } else {
                ValidationResult.Error(errorMessage)
            }
        }
    }

    data class Pattern(
        val regex: Regex,
        override val errorMessage: String
    ) : StringValidationRule() {
        override fun validate(value: String): ValidationResult {
            return if (regex.matches(value)) {
                ValidationResult.Success
            } else {
                ValidationResult.Error(errorMessage)
            }
        }
    }

    data class Email(
        override val errorMessage: String = "Invalid email address"
    ) : StringValidationRule() {
        private val emailRegex = Regex(
            "[a-zA-Z0-9+._%\\-]{1,256}" +
                    "@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
        )

        override fun validate(value: String): ValidationResult {
            return if (emailRegex.matches(value)) {
                ValidationResult.Success
            } else {
                ValidationResult.Error(errorMessage)
            }
        }
    }
}

/**
 * Number validation rules
 */
sealed class NumberValidationRule : ValidationRule<Number> {

    data class Minimum(
        val min: Number,
        override val errorMessage: String = "Value must be at least $min"
    ) : NumberValidationRule() {
        override fun validate(value: Number): ValidationResult {
            return if (value.toDouble() >= min.toDouble()) {
                ValidationResult.Success
            } else {
                ValidationResult.Error(errorMessage)
            }
        }
    }

    data class Maximum(
        val max: Number,
        override val errorMessage: String = "Value must be at most $max"
    ) : NumberValidationRule() {
        override fun validate(value: Number): ValidationResult {
            return if (value.toDouble() <= max.toDouble()) {
                ValidationResult.Success
            } else {
                ValidationResult.Error(errorMessage)
            }
        }
    }

    data class Range(
        val min: Number,
        val max: Number,
        override val errorMessage: String = "Value must be between $min and $max"
    ) : NumberValidationRule() {
        override fun validate(value: Number): ValidationResult {
            val doubleValue = value.toDouble()
            return if (doubleValue >= min.toDouble() && doubleValue <= max.toDouble()) {
                ValidationResult.Success
            } else {
                ValidationResult.Error(errorMessage)
            }
        }
    }
}

/**
 * Custom validation rule
 */
class CustomValidationRule<T>(
    private val validationFunction: (T) -> Boolean,
    override val errorMessage: String
) : ValidationRule<T>() {
    override fun validate(value: T): ValidationResult {
        return if (validationFunction(value)) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errorMessage)
        }
    }
}

/**
 * Validator class to run multiple validations
 */
class Validator<T> {
    private val rules = mutableListOf<ValidationRule<T>>()

    fun addRule(rule: ValidationRule<T>) = apply {
        rules.add(rule)
    }

    fun validate(value: T): ValidationResult {
        rules.forEach { rule ->
            val result = rule.validate(value)
            if (result is ValidationResult.Error) {
                return result
            }
        }
        return ValidationResult.Success
    }
}

// Extension function for easier validation
fun <T> T.validate(vararg rules: ValidationRule<T>): ValidationResult {
    return Validator<T>().apply {
        rules.forEach { addRule(it) }
    }.validate(this)
}

// Usage Examples
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
}

// Example Usage
fun validateJob(job: Job): ValidationResult {
    // Validate title
    job.title.validate(*JobValidation.titleRules.toTypedArray()).let {
        if (it is ValidationResult.Error) return it.copy(field = "title")
    }

    // Validate description
    job.description.validate(*JobValidation.descriptionRules.toTypedArray()).let {
        if (it is ValidationResult.Error) return it.copy(field = "description")
    }

    // Validate salary
    job.salary.validate(*JobValidation.salaryRules.toTypedArray()).let {
        if (it is ValidationResult.Error) return it.copy(field = "salary")
    }

    // Validate location
    JobValidation.locationValidator.validate(job.location).let {
        if (it is ValidationResult.Error) return it.copy(field = "location")
    }

    return ValidationResult.Success
}

// Example with multiple validations
fun validateEmail(email: String): ValidationResult {
    return Validator<String>()
        .addRule(StringValidationRule.NotEmpty("Email is required"))
        .addRule(StringValidationRule.Email())
        .validate(email)
}

// Example with custom validation
fun validatePassword(password: String): ValidationResult {
    return Validator<String>()
        .addRule(StringValidationRule.NotEmpty("Password is required"))
        .addRule(StringValidationRule.MinLength(8, "Password must be at least 8 characters"))
        .addRule(
            CustomValidationRule(
                { it.contains(Regex("[0-9]")) },
                "Password must contain at least one number"
            )
        )
        .addRule(
            CustomValidationRule(
                { it.contains(Regex("[A-Z]")) },
                "Password must contain at least one uppercase letter"
            )
        )
        .validate(password)
}