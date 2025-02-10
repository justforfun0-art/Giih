package com.example.gigwork.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.example.gigwork.core.error.extensions.toAppError
import com.example.gigwork.core.error.handler.GlobalErrorHandler
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.error.model.ErrorLevel
import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.core.result.Result
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.models.Location
import com.example.gigwork.domain.usecase.job.CreateJobUseCase
import com.example.gigwork.presentation.base.BaseErrorViewModel
import com.example.gigwork.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import com.example.gigwork.domain.repository.LocationRepository
import com.example.gigwork.presentation.states.CreateJobEvent
import com.example.gigwork.presentation.states.CreateJobUiState
import kotlinx.coroutines.CoroutineDispatcher
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.repository.JobDraftRepository
import com.example.gigwork.presentation.base.ErrorHandler
import com.example.gigwork.presentation.states.FieldErrorType
import com.example.gigwork.presentation.states.ValidationError
import com.example.gigwork.presentation.states.JobDraft
import java.util.UUID

@HiltViewModel
class CreateJobViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    errorHandler: ErrorHandler,
    private val createJobUseCase: CreateJobUseCase,
    private val jobDraftRepository: JobDraftRepository,
    private val locationRepository: LocationRepository,
    logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BaseErrorViewModel<CreateJobUiState, CreateJobEvent>(savedStateHandle, errorHandler, logger) {
    override fun createInitialState(): CreateJobUiState = CreateJobUiState()

    companion object {
        private const val TAG = "CreateJobViewModel"
        private const val MIN_TITLE_LENGTH = 5
        private const val MAX_TITLE_LENGTH = 100
        private const val MIN_DESCRIPTION_LENGTH = 20
        private const val MAX_DESCRIPTION_LENGTH = 1000
        private const val MAX_SALARY = 1000000.0
        private const val MIN_DURATION = 1
        private const val MAX_DURATION = 365

        private const val KEY_TITLE = "title"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_SALARY = "salary"
        private const val KEY_SALARY_UNIT = "salaryUnit"
        private const val KEY_WORK_DURATION = "workDuration"
        private const val KEY_WORK_DURATION_UNIT = "workDurationUnit"
        private const val KEY_STATE = "state"
        private const val KEY_DISTRICT = "district"
        private const val KEY_DRAFT_ID = "draftId"
    }

    private val _focusedField = MutableStateFlow<String?>(null)
    val focusedField = _focusedField.asStateFlow()

    private val _previewMode = MutableStateFlow(false)
    val previewMode = _previewMode.asStateFlow()

    private val draftId = savedStateHandle.get<String>(KEY_DRAFT_ID)

    init {
        initializeState()
    }

    private fun initializeState() = safeLaunch {
        if (draftId != null) {
            loadDraft(draftId)
        } else {
            restoreState()
        }
        loadLocationData()
    }

    // Add these internal classes
    sealed class FieldValidationResult {
        object Valid : FieldValidationResult()
        data class Invalid(val errorMessage: String, val errorType: FieldErrorType) : FieldValidationResult()
    }

    enum class FieldErrorType {
        REQUIRED,
        TOO_SHORT,
        TOO_LONG,
        PATTERN_MISMATCH,
        INVALID_VALUE
    }

    private fun isRequiredField(field: String): Boolean = field in setOf(
        "title",
        "description",
        "salary",
        "workDuration",
        "state",
        "district"
    )

    private fun handleValidationError(field: String, message: String, errorType: FieldErrorType) {
        setState {
            copy(
                validationErrors = validationErrors + (field to ValidationError(
                    message = message,
                    errorType = errorType,
                    field = field
                ))
            )
        }
    }

    private suspend fun loadDraft(draftId: String) {
        jobDraftRepository.getDraft(draftId)?.let { draft ->
            setState {
                copy(
                    title = draft.title,
                    description = draft.description,
                    salary = draft.salary.toString(),
                    salaryUnit = draft.salaryUnit,
                    workDuration = draft.workDuration.toString(),
                    workDurationUnit = draft.workDurationUnit,
                    state = draft.location.state,
                    district = draft.location.district,
                    latitude = draft.location.latitude,
                    longitude = draft.location.longitude,
                    isDraft = true
                )
            }
            validateAllFields()
        }
    }

    private fun restoreState() {
        setState {
            copy(
                title = getSavedState(KEY_TITLE, ""),
                description = getSavedState(KEY_DESCRIPTION, ""),
                salary = getSavedState(KEY_SALARY, ""),
                salaryUnit = getSavedState(KEY_SALARY_UNIT, "monthly"),
                workDuration = getSavedState(KEY_WORK_DURATION, ""),
                workDurationUnit = getSavedState(KEY_WORK_DURATION_UNIT, "days"),
                state = getSavedState(KEY_STATE, ""),
                district = getSavedState(KEY_DISTRICT, "")
            )
        }
    }

    private suspend fun loadLocationData() {
        LocationRepository.getStates().collect { states ->
            setState { copy(availableStates = states) }
        }
        state.value.state?.let { selectedState ->
            loadDistricts(selectedState)
        }
    }

    private suspend fun loadDistricts(state: String) {
        locationRepository.getDistricts(state).collect { districts ->
            setState { copy(availableDistricts = districts) }
        }
    }

    // Field Update Functions
    fun updateTitle(title: String) {
        setSavedState(KEY_TITLE, title)
        setState { copy(title = title) }
        validateField("title", title)
        autosaveDraft()
    }

    fun updateDescription(description: String) {
        setSavedState(KEY_DESCRIPTION, description)
        setState { copy(description = description) }
        validateField("description", description)
        autosaveDraft()
    }

    fun updateSalary(salary: String) {
        setSavedState(KEY_SALARY, salary)
        setState { copy(salary = salary) }
        validateField("salary", salary)
        updateTotalCost()
        autosaveDraft()
    }

    fun updateSalaryUnit(unit: String) {
        setSavedState(KEY_SALARY_UNIT, unit)
        setState { copy(salaryUnit = unit) }
        validateSalaryUnit(unit)
        updateTotalCost()
        autosaveDraft()
    }

    fun updateWorkDuration(duration: String) {
        setSavedState(KEY_WORK_DURATION, duration)
        setState { copy(workDuration = duration) }
        validateField("workDuration", duration)
        updateTotalCost()
        autosaveDraft()
    }

    fun updateWorkDurationUnit(unit: String) {
        setSavedState(KEY_WORK_DURATION_UNIT, unit)
        setState { copy(workDurationUnit = unit) }
        validateDurationUnit(unit)
        updateTotalCost()
        autosaveDraft()
    }

    fun updateLocation(state: String, district: String) = safeLaunch {
        setSavedState(KEY_STATE, state)
        setSavedState(KEY_DISTRICT, district)
        setState {
            copy(
                state = state,
                district = district
            )
        }
        loadDistricts(state)
        validateLocation(state, district)
        autosaveDraft()
    }

    fun updateCoordinates(latitude: Double?, longitude: Double?) {
        setState {
            copy(
                latitude = latitude,
                longitude = longitude
            )
        }
        autosaveDraft()
    }

    // Field Focus Handling
    fun onFieldFocused(fieldName: String) {
        _focusedField.value = fieldName
    }

    fun onFieldBlurred(fieldName: String) {
        if (_focusedField.value == fieldName) {
            _focusedField.value = null
            validateField(fieldName, getFieldValue(fieldName))
        }
    }

    private fun getFieldValue(fieldName: String): String {
        return when (fieldName) {
            "title" -> state.value.title
            "description" -> state.value.description
            "salary" -> state.value.salary
            "workDuration" -> state.value.workDuration
            else -> ""
        }
    }

    // Validation Functions
    private fun validateField(field: String, value: String): FieldValidationResult {
        return when {
            value.isBlank() && isRequiredField(field) -> {
                handleValidationError(field, "This field is required", FieldErrorType.REQUIRED)
                FieldValidationResult.Invalid("This field is required", FieldErrorType.REQUIRED)
            }
            field == "title" -> validateTitle(value)
            field == "description" -> validateDescription(value)
            field == "salary" -> validateSalary(value)
            field == "workDuration" -> validateWorkDuration(value)
            else -> FieldValidationResult.Valid
        }
    }

    private fun validateTitle(value: String): FieldValidationResult {
        return when {
            value.length < MIN_TITLE_LENGTH -> {
                FieldValidationResult.Invalid(
                    "Title must be at least $MIN_TITLE_LENGTH characters",
                    FieldErrorType.TOO_SHORT
                )
            }
            value.length > MAX_TITLE_LENGTH -> {
                FieldValidationResult.Invalid(
                    "Title cannot exceed $MAX_TITLE_LENGTH characters",
                    FieldErrorType.TOO_LONG
                )
            }
            !value.matches(Regex("^[a-zA-Z0-9\\s.,!?-]+$")) -> {
                FieldValidationResult.Invalid(
                    "Title contains invalid characters",
                    FieldErrorType.PATTERN_MISMATCH
                )
            }
            else -> FieldValidationResult.Valid
        }.also { result ->
            if (result is FieldValidationResult.Invalid) {
                handleValidationError("title", result.errorMessage, result.errorType)
            } else {
                clearFieldError("title")
            }
        }
    }

    private fun validateDescription(value: String): FieldValidationResult {
        return when {
            value.length < MIN_DESCRIPTION_LENGTH -> {
                FieldValidationResult.Invalid(
                    "Description must be at least $MIN_DESCRIPTION_LENGTH characters",
                    FieldErrorType.TOO_SHORT
                )
            }
            value.length > MAX_DESCRIPTION_LENGTH -> {
                FieldValidationResult.Invalid(
                    "Description cannot exceed $MAX_DESCRIPTION_LENGTH characters",
                    FieldErrorType.TOO_LONG
                )
            }
            else -> FieldValidationResult.Valid
        }.also { result ->
            if (result is FieldValidationResult.Invalid) {
                handleValidationError("description", result.errorMessage, result.errorType)
            } else {
                clearFieldError("description")
            }
        }
    }

    private fun validateSalary(value: String): FieldValidationResult {
        val salary = value.toDoubleOrNull()
        return when {
            salary == null -> {
                FieldValidationResult.Invalid(
                    "Please enter a valid salary amount",
                    FieldErrorType.INVALID_VALUE
                )
            }
            salary <= 0 -> {
                FieldValidationResult.Invalid(
                    "Salary must be greater than 0",
                    FieldErrorType.INVALID_VALUE
                )
            }
            salary > MAX_SALARY -> {
                FieldValidationResult.Invalid(
                    "Salary cannot exceed ${MAX_SALARY.formatAsCurrency()}",
                    FieldErrorType.INVALID_VALUE
                )
            }
            else -> FieldValidationResult.Valid
        }.also { result ->
            if (result is FieldValidationResult.Invalid) {
                handleValidationError("salary", result.errorMessage, result.errorType)
            } else {
                clearFieldError("salary")
            }
        }
    }

    private fun validateWorkDuration(value: String): FieldValidationResult {
        val duration = value.toIntOrNull()
        return when {
            duration == null -> {
                FieldValidationResult.Invalid(
                    "Please enter a valid duration",
                    FieldErrorType.INVALID_VALUE
                )
            }
            duration < MIN_DURATION -> {
                FieldValidationResult.Invalid(
                    "Duration must be at least $MIN_DURATION",
                    FieldErrorType.INVALID_VALUE
                )
            }
            duration > MAX_DURATION -> {
                FieldValidationResult.Invalid(
                    "Duration cannot exceed $MAX_DURATION",
                    FieldErrorType.INVALID_VALUE
                )
            }
            else -> FieldValidationResult.Valid
        }.also { result ->
            if (result is FieldValidationResult.Invalid) {
                handleValidationError("workDuration", result.errorMessage, result.errorType)
            } else {
                clearFieldError("workDuration")
            }
        }
    }

// ... continuing from previous implementation

    private fun validateSalaryUnit(unit: String) {
        val validUnits = setOf("hourly", "daily", "weekly", "monthly")
        if (unit !in validUnits) {
            handleValidationError(
                "salaryUnit",
                "Invalid salary unit. Must be one of: ${validUnits.joinToString(", ")}",
                FieldErrorType.INVALID_VALUE
            )
        } else {
            clearFieldError("salaryUnit")
        }
    }

    private fun validateDurationUnit(unit: String) {
        val validUnits = setOf("hours", "days", "weeks", "months")
        if (unit !in validUnits) {
            handleValidationError(
                "workDurationUnit",
                "Invalid duration unit. Must be one of: ${validUnits.joinToString(", ")}",
                FieldErrorType.INVALID_VALUE
            )
        } else {
            clearFieldError("workDurationUnit")
        }
    }

    private fun validateLocation(state: String, district: String) {
        when {
            state.isBlank() -> {
                handleValidationError("location", "Please select a state", FieldErrorType.REQUIRED)
            }
            district.isBlank() -> {
                handleValidationError("location", "Please select a district", FieldErrorType.REQUIRED)
            }
            else -> clearFieldError("location")
        }
    }

    private fun validateAllFields() {
        val currentState = state.value
        validateField("title", currentState.title)
        validateField("description", currentState.description)
        validateField("salary", currentState.salary)
        validateField("workDuration", currentState.workDuration)
        validateSalaryUnit(currentState.salaryUnit)
        validateDurationUnit(currentState.workDurationUnit)
        validateLocation(currentState.state, currentState.district)
    }

    // Job Creation and Submission
    fun createJob() = safeLaunch {
        try {
            validateAllFields()
            if (state.value.validationErrors.isNotEmpty()) {
                emitEvent(CreateJobEvent.ValidationError(state.value.validationErrors))
                return@safeLaunch
            }

            setState { copy(isLoading = true) }

            val job = createJobModel()
            createJobUseCase(job).collect { result ->
                when (result) {
                    is Result.Success -> handleSuccess(result.data)
                    is Result.Error -> handleError(result.error)
                    is Result.Loading -> setState { copy(isLoading = true) }
                }
            }
        } catch (e: Exception) {
            handleError(e.toAppError())
        }
    }

    // Draft Management
    private fun autosaveDraft() = safeLaunch {
        if (!state.value.isValid() || !state.value.isDirty) return@safeLaunch

        val draft = createDraftModel()
        jobDraftRepository.saveDraft(draft)
        setState { copy(isDraft = true, lastSavedAt = System.currentTimeMillis()) }
    }

    fun saveDraft() = safeLaunch {
        try {
            val draft = createDraftModel()
            jobDraftRepository.saveDraft(draft)
            setState {
                copy(
                    isDraft = true,
                    lastSavedAt = System.currentTimeMillis(),
                    successMessage = "Draft saved successfully"
                )
            }
        } catch (e: Exception) {
            handleError(e.toAppError())
        }
    }

    fun deleteDraft() = safeLaunch {
        try {
            draftId?.let {
                jobDraftRepository.deleteDraft(it)
                setState { copy(isDraft = false) }
                emitEvent(CreateJobEvent.DraftDeleted)
            }
        } catch (e: Exception) {
            handleError(e.toAppError())
        }
    }

    // Preview Management
    fun togglePreview() {
        _previewMode.value = !_previewMode.value
    }

    fun exitPreview() {
        _previewMode.value = false
    }

    // Cost Calculations
    private fun updateTotalCost() {
        val salary = state.value.salary.toDoubleOrNull() ?: 0.0
        val duration = state.value.workDuration.toIntOrNull() ?: 0

        val totalCost = calculateTotalCost(
            salary,
            state.value.salaryUnit,
            duration,
            state.value.workDurationUnit
        )

        setState { copy(totalCost = totalCost) }
    }

    private fun calculateTotalCost(
        salary: Double,
        salaryUnit: String,
        duration: Int,
        durationUnit: String
    ): Double {
        return when (salaryUnit) {
            "hourly" -> calculateHourlyTotal(salary, duration, durationUnit)
            "daily" -> calculateDailyTotal(salary, duration, durationUnit)
            "weekly" -> calculateWeeklyTotal(salary, duration, durationUnit)
            "monthly" -> calculateMonthlyTotal(salary, duration, durationUnit)
            else -> 0.0
        }
    }

    private fun calculateHourlyTotal(salary: Double, duration: Int, durationUnit: String): Double {
        return when (durationUnit) {
            "hours" -> salary * duration
            "days" -> salary * duration * 8 // Assuming 8-hour work day
            "weeks" -> salary * duration * 40 // Assuming 40-hour work week
            "months" -> salary * duration * 160 // Assuming 160-hour work month
            else -> 0.0
        }
    }

    private fun calculateDailyTotal(salary: Double, duration: Int, durationUnit: String): Double {
        return when (durationUnit) {
            "hours" -> salary * (duration / 8.0)
            "days" -> salary * duration
            "weeks" -> salary * duration * 5 // Assuming 5-day work week
            "months" -> salary * duration * 22 // Assuming 22 working days per month
            else -> 0.0
        }
    }

    private fun calculateWeeklyTotal(salary: Double, duration: Int, durationUnit: String): Double {
        return when (durationUnit) {
            "hours" -> salary * (duration / 40.0)
            "days" -> salary * (duration / 5.0)
            "weeks" -> salary * duration
            "months" -> salary * duration * 4.33 // Average weeks per month
            else -> 0.0
        }
    }

    private fun calculateMonthlyTotal(salary: Double, duration: Int, durationUnit: String): Double {
        return when (durationUnit) {
            "hours" -> salary * (duration / 160.0)
            "days" -> salary * (duration / 22.0)
            "weeks" -> salary * (duration / 4.33)
            "months" -> salary * duration
            else -> 0.0
        }
    }

    // Helper Functions
    private fun createJobModel(): Job {
        val currentState = state.value
        return Job(
            id = "",  // Will be assigned by backend
            title = currentState.title.trim(),
            description = currentState.description.trim(),
            employerId = getSavedState("userId", ""),
            location = Location(
                state = currentState.state,
                district = currentState.district,
                latitude = currentState.latitude,
                longitude = currentState.longitude
            ),
            salary = currentState.salary.toDoubleOrNull() ?: 0.0,
            salaryUnit = currentState.salaryUnit,
            workDuration = currentState.workDuration.toIntOrNull() ?: 0,
            workDurationUnit = currentState.workDurationUnit,
            status = "OPEN",
            createdAt = ""  // Will be assigned by backend
        )
    }


    private fun createDraftModel(): JobDraft {
        val currentState = state.value
        return JobDraft(
            id = draftId ?: UUID.randomUUID().toString(),
            title = currentState.title,
            description = currentState.description,
            salary = currentState.salary.toDoubleOrNull() ?: 0.0,
            salaryUnit = currentState.salaryUnit,
            workDuration = currentState.workDuration.toIntOrNull() ?: 0,
            workDurationUnit = currentState.workDurationUnit,
            location = Location(
                state = currentState.state,
                district = currentState.district,
                latitude = currentState.latitude,
                longitude = currentState.longitude
            ),
            lastModified = System.currentTimeMillis()
        )
    }

    // State Cleanup
    override fun onCleared() {
        viewModelScope.launch {
            if (state.value.isDraft) {
                saveDraft()
            }
        }
        clearSavedState()
        super.onCleared()
    }

    private fun clearSavedState() {
        clearSavedState(KEY_TITLE)
        clearSavedState(KEY_DESCRIPTION)
        clearSavedState(KEY_SALARY)
        clearSavedState(KEY_SALARY_UNIT)
        clearSavedState(KEY_WORK_DURATION)
        clearSavedState(KEY_WORK_DURATION_UNIT)
        clearSavedState(KEY_STATE)
        clearSavedState(KEY_DISTRICT)
        clearSavedState(KEY_DRAFT_ID)
    }

    // Error Handling
    override fun handleError(error: AppError) {
        val errorMessage = when (error) {
            is AppError.ValidationError -> ErrorMessage(
                message = error.message,
                title = "Validation Error",
                level = ErrorLevel.WARNING
            )
            is AppError.NetworkError -> ErrorMessage(
                message = "Unable to create job. Please check your connection.",
                title = "Network Error",
                level = ErrorLevel.ERROR
            )
            else -> ErrorMessage(
                message = error.message,
                title = "Error",
                level = ErrorLevel.ERROR
            )
        }

        setState {
            copy(
                isLoading = false,
                errorMessage = errorMessage
            )
        }
    }

    private fun handleSuccess(job: Job) {
        setState {
            copy(
                isLoading = false,
                isSuccess = true,
                errorMessage = null
            )
        }
        clearSavedState()
        safeLaunch {
            draftId?.let { jobDraftRepository.deleteDraft(it) }
            emitEvent(CreateJobEvent.JobCreated(job.id))
        }
    }

    fun retry() {
        createJob()
    }

    fun clearError() {
        setState {
            copy(
                errorMessage = null,
                validationErrors = emptyMap()
            )
        }
    }
}