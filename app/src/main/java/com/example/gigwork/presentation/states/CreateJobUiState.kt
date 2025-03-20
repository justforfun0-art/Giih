package com.example.gigwork.presentation.states

import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.domain.models.Location
import com.example.gigwork.presentation.base.UiEvent
import com.example.gigwork.presentation.base.UiState

data class CreateJobUiState(
    val title: String = "",
    val description: String = "",
    val salary: String = "",
    val salaryUnit: String = "monthly",
    val workDuration: String = "",
    val workDurationUnit: String = "days",
    val state: String = "",
    val district: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val availableStates: List<String> = emptyList(),
    val availableDistricts: List<String> = emptyList(),
    val validationErrors: Map<String, ValidationError> = emptyMap(),
    override val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    override val errorMessage: ErrorMessage? = null,
    val successMessage: String? = null,
    val isDraft: Boolean = false,
    val isPreviewMode: Boolean = false,
    val lastSavedAt: Long? = null,
    val totalCost: Double = 0.0,
    val costBreakdown: Map<String, Double> = emptyMap(),
    val isDirty: Boolean = false,
    val isSubmitting: Boolean = false,
    val isLoadingLocation: Boolean = false
) : UiState<CreateJobUiState> {

    override fun copy(
        isLoading: Boolean,
        errorMessage: ErrorMessage?
    ): CreateJobUiState {
        return copy(
            isLoading = isLoading,
            errorMessage = errorMessage
        )
    }

    fun isValid(): Boolean = validationErrors.isEmpty() &&
            title.isNotBlank() &&
            description.isNotBlank() &&
            salary.isNotBlank() &&
            workDuration.isNotBlank() &&
            state.isNotBlank() &&
            district.isNotBlank()
    fun hasErrors(): Boolean = validationErrors.isNotEmpty()

    fun getError(field: String): ValidationError? = validationErrors[field]

    fun isFieldValid(field: String): Boolean = !validationErrors.containsKey(field)

    fun getFieldError(field: String): String? = validationErrors[field]?.message

    fun canSubmit(): Boolean = isValid() && !isLoading && !isSubmitting
}

data class ValidationError(
    val message: String,
    val errorType: FieldErrorType,
    val field: String = ""
)

enum class FieldErrorType {
    REQUIRED,
    TOO_SHORT,
    TOO_LONG,
    PATTERN_MISMATCH,
    INVALID_VALUE,
    INVALID_FORMAT,
    OUT_OF_RANGE
}

sealed class CreateJobEvent : UiEvent {
    object NavigateBack : CreateJobEvent()
    data class JobCreated(val jobId: String) : CreateJobEvent()
    data class ValidationError(val errors: Map<String, com.example.gigwork.presentation.states.ValidationError>) : CreateJobEvent()    object DraftSaved : CreateJobEvent()
    object DraftDeleted : CreateJobEvent()
    data class ShowSnackbar(val message: String) : CreateJobEvent()
    object PreviewToggled : CreateJobEvent()
    data class LocationSelected(val state: String, val district: String) : CreateJobEvent()
    data class CoordinatesUpdated(val latitude: Double, val longitude: Double) : CreateJobEvent()
    data class CostCalculated(val total: Double, val breakdown: Map<String, Double>) : CreateJobEvent()
}

data class JobFormData(
    val title: String,
    val description: String,
    val salary: Double,
    val salaryUnit: String,
    val workDuration: Int,
    val workDurationUnit: String,
    val location: LocationData
)

data class LocationData(
    val state: String,
    val district: String,
    val latitude: Double?,
    val longitude: Double?
)

data class JobDraft(
    val id: String,
    val title: String,
    val description: String,
    val salary: Double,
    val salaryUnit: String,
    val workDuration: Int,
    val workDurationUnit: String,
    val location: Location,
    val lastModified: Long
)

sealed class FormFieldState {
    object Initial : FormFieldState()
    object Focused : FormFieldState()
    object Valid : FormFieldState()
    data class Error(val error: ValidationError) : FormFieldState()
}

data class FieldState<T>(
    val value: T,
    val state: FormFieldState = FormFieldState.Initial,
    val isTouched: Boolean = false,
    val isDirty: Boolean = false
)

data class CostBreakdown(
    val baseAmount: Double,
    val periodAmount: Double,
    val totalAmount: Double,
    val perPeriod: String,
    val totalPeriods: Int
)

object ValidationConstants {
    const val MIN_TITLE_LENGTH = 5
    const val MAX_TITLE_LENGTH = 100
    const val MIN_DESCRIPTION_LENGTH = 20
    const val MAX_DESCRIPTION_LENGTH = 1000
    const val MIN_SALARY = 0.0
    const val MAX_SALARY = 1000000.0
    const val MIN_DURATION = 1
    const val MAX_DURATION = 365

    val VALID_SALARY_UNITS = setOf("hourly", "daily", "weekly", "monthly")
    val VALID_DURATION_UNITS = setOf("hours", "days", "weeks", "months")

    val TITLE_REGEX = Regex("^[a-zA-Z0-9\\s.,!?-]+$")
    val SALARY_REGEX = Regex("^\\d+(\\.\\d{0,2})?$")
    val DURATION_REGEX = Regex("^\\d+$")
}

object FormFields {
    const val TITLE = "title"
    const val DESCRIPTION = "description"
    const val SALARY = "salary"
    const val SALARY_UNIT = "salaryUnit"
    const val WORK_DURATION = "workDuration"
    const val WORK_DURATION_UNIT = "workDurationUnit"
    const val STATE = "state"
    const val DISTRICT = "district"
    const val LOCATION = "location"
}