package com.example.gigwork.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.gigwork.presentation.base.AppError
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.models.Location
import com.example.gigwork.domain.repository.JobDraftRepository
import com.example.gigwork.domain.repository.LocationRepository
import com.example.gigwork.domain.usecase.job.CreateJobUseCase
import com.example.gigwork.presentation.base.BaseErrorViewModel
import com.example.gigwork.core.error.handler.GlobalErrorHandler
import com.example.gigwork.presentation.states.*
import com.example.gigwork.presentation.base.Logger
import com.example.gigwork.presentation.base.ErrorHandler
import com.example.gigwork.domain.validation.ValidationError as DomainValidationError
import com.example.gigwork.presentation.states.ValidationError as UiValidationError
import com.example.gigwork.core.result.Result
import com.example.gigwork.domain.validation.ValidationError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject
import com.example.gigwork.domain.validation.Validators
import com.example.gigwork.presentation.states.CreateJobEvent
import com.example.gigwork.core.result.ApiResult



@HiltViewModel
class CreateJobViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val errorHandler: ErrorHandler,
    private val createJobUseCase: CreateJobUseCase,
    private val jobDraftRepository: JobDraftRepository,
    private val locationRepository: LocationRepository,
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BaseErrorViewModel<CreateJobUiState, CreateJobEvent>(savedStateHandle, errorHandler, logger) {

    private fun DomainValidationError.toUiValidationError(): UiValidationError {
        return UiValidationError(
            message = this.message,
            errorType = FieldErrorType.INVALID_VALUE,
            field = this.field
        )
    }

    override fun createInitialState(): CreateJobUiState = CreateJobUiState()

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState = _uiState.asStateFlow()
    private val draftId = savedStateHandle.get<String>(KEY_DRAFT_ID)
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    private var autosaveJob: kotlinx.coroutines.Job? = null
    private val _previewMode = MutableStateFlow(false)
    val previewMode = _previewMode.asStateFlow()

    init {
        initializeState()
    }

    // State Management
    override fun setState(update: CreateJobUiState.() -> CreateJobUiState) {
        _uiState.update { it.update() }
    }

    private fun getState(): CreateJobUiState = _uiState.value

    private fun initializeState() {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            try {
                if (draftId != null) {
                    loadDraft(draftId)
                }
                loadLocationData()
            } catch (e: Exception) {
                handleError(e as AppError)
            } finally {
                setState { copy(isLoading = false) }
            }
        }
    }

    // Add this conversion function in CreateJobViewModel


    private suspend fun loadDraft(draftId: String) {
        try {
            jobDraftRepository.getDraft(draftId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        // Since we know the repository returns JobDraft, we can safely cast
                        (result.data as? JobDraft)?.let { draft ->
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
                                    isDraft = true,
                                    lastSavedAt = draft.lastModified,
                                    isDirty = false
                                )
                            }
                            validateAllFields()
                        }
                    }
                    is ApiResult.Error -> {
                        handleError(AppError.Database(
                            message = "Failed to load draft",
                            cause = result.error.cause
                        ))
                    }
                    is ApiResult.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                }
            }
        } catch (e: Exception) {
            handleError(AppError.Database(
                message = "Failed to load draft",
                cause = e
            ))
        } finally {
            setState { copy(isLoading = false) }
        }
    }

    // Field Update Functions
    fun updateTitle(title: String) {
        setSavedState(KEY_TITLE, title)
        setState { copy(title = title, isDirty = true) }
        validateField("title", title)
        triggerAutosave()
    }

// Continue with the rest of the implementation...
// Field Update Functions (continued)
fun updateDescription(description: String) {
    setSavedState(KEY_DESCRIPTION, description)
    setState { copy(description = description, isDirty = true) }
    validateField("description", description)
    triggerAutosave()
}

    fun updateSalary(salary: String) {
        setSavedState(KEY_SALARY, salary)
        setState { copy(salary = salary, isDirty = true) }
        validateField("salary", salary)
        updateTotalCost()
        triggerAutosave()
    }

    fun updateSalaryUnit(unit: String) {
        setSavedState(KEY_SALARY_UNIT, unit)
        setState { copy(salaryUnit = unit, isDirty = true) }
        validateSalaryUnit(unit)
        updateTotalCost()
        triggerAutosave()
    }

    fun updateWorkDuration(duration: String) {
        setSavedState(KEY_WORK_DURATION, duration)
        setState { copy(workDuration = duration, isDirty = true) }
        validateField("workDuration", duration)
        updateTotalCost()
        triggerAutosave()
    }

    fun updateWorkDurationUnit(unit: String) {
        setSavedState(KEY_WORK_DURATION_UNIT, unit)
        setState { copy(workDurationUnit = unit, isDirty = true) }
        validateDurationUnit(unit)
        updateTotalCost()
        triggerAutosave()
    }

    fun updateLocation(state: String, district: String) {
        viewModelScope.launch {
            setSavedState(KEY_STATE, state)
            setSavedState(KEY_DISTRICT, district)
            setState {
                copy(
                    state = state,
                    district = district,
                    isDirty = true
                )
            }
            loadDistricts(state)
            validateLocation(state, district)
            triggerAutosave()
        }
    }

    fun updateCoordinates(latitude: Double?, longitude: Double?) {
        setState {
            copy(
                latitude = latitude,
                longitude = longitude,
                isDirty = true
            )
        }
        triggerAutosave()
    }

    // Validation Functions
    private sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val message: String, val errorType: FieldErrorType) : ValidationResult()
    }

    private fun validateField(field: String, value: String) {
        val validationError = when (field) {
            "title" -> Validators.validateJobTitle(value)
            "description" -> Validators.validateJobDescription(value)
            "salary" -> validateSalaryField(value)
            "workDuration" -> validateWorkDurationField(value)
            else -> null
        }

        updateValidationState(field, validationError)
    }
    private fun validateSalaryField(value: String): ValidationError? {
        val salary = value.toDoubleOrNull()
        return when {
            value.isBlank() -> ValidationError("Salary is required", "salary")
            salary == null -> ValidationError("Please enter a valid salary amount", "salary")
            else -> Validators.validateSalary(salary, getState().salaryUnit)
        }
    }

    private fun validateWorkDurationField(value: String): ValidationError? {
        val duration = value.toIntOrNull()
        return when {
            value.isBlank() -> ValidationError("Duration is required", "workDuration")
            duration == null -> ValidationError("Please enter a valid duration", "workDuration")
            else -> Validators.validateWorkDuration(duration, getState().workDurationUnit)
        }
    }

    private fun validateLocation(state: String, district: String) {
        val location = Location(
            state = state,
            district = district,
            latitude = getState().latitude,
            longitude = getState().longitude,
            address = null,
            pinCode = null
        )

        val validationError = Validators.validateLocation(location)
        updateValidationState("location", validationError)
    }

    private fun validateSalaryUnit(unit: String) {
        val validationError = if (unit !in Validators.ALLOWED_SALARY_UNITS) {
            ValidationError(
                "Invalid salary unit. Allowed values: ${Validators.ALLOWED_SALARY_UNITS.joinToString(", ")}",
                "salaryUnit"
            )
        } else null
        updateValidationState("salaryUnit", validationError)
    }

    private fun validateDurationUnit(unit: String) {
        val validationError = if (unit !in Validators.ALLOWED_DURATION_UNITS) {
            ValidationError(
                "Invalid duration unit. Allowed values: ${Validators.ALLOWED_DURATION_UNITS.joinToString(", ")}",
                "workDurationUnit"
            )
        } else null
        updateValidationState("workDurationUnit", validationError)
    }

    private fun updateValidationState(field: String, error: DomainValidationError?) {
        setState {
            val newErrors = validationErrors.toMutableMap()
            if (error != null) {
                newErrors[field] = error.toUiValidationError()
            } else {
                newErrors.remove(field)
            }
            copy(validationErrors = newErrors)
        }
    }

    private fun validateTitle(value: String): ValidationResult {
        return when {
            value.isBlank() -> ValidationResult.Invalid(
                "Title is required",
                FieldErrorType.REQUIRED
            )
            value.length < MIN_TITLE_LENGTH -> ValidationResult.Invalid(
                "Title must be at least $MIN_TITLE_LENGTH characters",
                FieldErrorType.TOO_SHORT
            )
            value.length > MAX_TITLE_LENGTH -> ValidationResult.Invalid(
                "Title cannot exceed $MAX_TITLE_LENGTH characters",
                FieldErrorType.TOO_LONG
            )
            !value.matches(Regex("^[a-zA-Z0-9\\s.,!?-]+$")) -> ValidationResult.Invalid(
                "Title contains invalid characters",
                FieldErrorType.PATTERN_MISMATCH
            )
            else -> ValidationResult.Valid
        }
    }

    private fun validateDescription(value: String): ValidationResult {
        return when {
            value.isBlank() -> ValidationResult.Invalid(
                "Description is required",
                FieldErrorType.REQUIRED
            )
            value.length < MIN_DESCRIPTION_LENGTH -> ValidationResult.Invalid(
                "Description must be at least $MIN_DESCRIPTION_LENGTH characters",
                FieldErrorType.TOO_SHORT
            )
            value.length > MAX_DESCRIPTION_LENGTH -> ValidationResult.Invalid(
                "Description cannot exceed $MAX_DESCRIPTION_LENGTH characters",
                FieldErrorType.TOO_LONG
            )
            else -> ValidationResult.Valid
        }
    }

    private fun validateSalary(value: String): ValidationResult {
        val salary = value.toDoubleOrNull()
        return when {
            value.isBlank() -> ValidationResult.Invalid(
                "Salary is required",
                FieldErrorType.REQUIRED
            )
            salary == null -> ValidationResult.Invalid(
                "Please enter a valid salary amount",
                FieldErrorType.INVALID_VALUE
            )
            salary <= 0 -> ValidationResult.Invalid(
                "Salary must be greater than 0",
                FieldErrorType.INVALID_VALUE
            )
            salary > MAX_SALARY -> ValidationResult.Invalid(
                "Salary cannot exceed ${currencyFormatter.format(MAX_SALARY)}",
                FieldErrorType.INVALID_VALUE
            )
            else -> ValidationResult.Valid
        }
    }

// I will continue with the remaining implementation...
// Validation Functions (continued)
private fun validateWorkDuration(value: String): ValidationResult {
    val duration = value.toIntOrNull()
    return when {
        value.isBlank() -> ValidationResult.Invalid(
            "Duration is required",
            FieldErrorType.REQUIRED
        )
        duration == null -> ValidationResult.Invalid(
            "Please enter a valid duration",
            FieldErrorType.INVALID_VALUE
        )
        duration < MIN_DURATION -> ValidationResult.Invalid(
            "Duration must be at least $MIN_DURATION",
            FieldErrorType.INVALID_VALUE
        )
        duration > MAX_DURATION -> ValidationResult.Invalid(
            "Duration cannot exceed $MAX_DURATION",
            FieldErrorType.INVALID_VALUE
        )
        else -> ValidationResult.Valid
    }
}



    private fun validateAllFields() {
        with(_uiState.value) {
            validateField("title", title)
            validateField("description", description)
            validateField("salary", salary)
            validateField("workDuration", workDuration)
            validateSalaryUnit(salaryUnit)
            validateDurationUnit(workDurationUnit)
            validateLocation(state, district)
        }
    }

    // Cost Calculation Functions
    private fun updateTotalCost() {
        val state = getState()
        val salary = state.salary.toDoubleOrNull() ?: 0.0
        val duration = state.workDuration.toIntOrNull() ?: 0

        val totalCost = calculateTotalCost(
            salary = salary,
            salaryUnit = state.salaryUnit,
            duration = duration,
            durationUnit = state.workDurationUnit
        )

        setState { copy(totalCost = totalCost) }
    }

    private fun calculateTotalCost(
        salary: Double,
        salaryUnit: String,
        duration: Int,
        durationUnit: String
    ): Double {
        return when (salaryUnit.lowercase()) {
            "hourly" -> calculateHourlyTotal(salary, duration, durationUnit)
            "daily" -> calculateDailyTotal(salary, duration, durationUnit)
            "weekly" -> calculateWeeklyTotal(salary, duration, durationUnit)
            "monthly" -> calculateMonthlyTotal(salary, duration, durationUnit)
            else -> 0.0
        }
    }

    private fun calculateHourlyTotal(salary: Double, duration: Int, durationUnit: String): Double {
        return when (durationUnit.lowercase()) {
            "hours" -> salary * duration
            "days" -> salary * duration * 8 // 8-hour work day
            "weeks" -> salary * duration * 40 // 40-hour work week
            "months" -> salary * duration * 160 // 160-hour work month
            else -> 0.0
        }
    }

    private fun calculateDailyTotal(salary: Double, duration: Int, durationUnit: String): Double {
        return when (durationUnit.lowercase()) {
            "hours" -> salary * (duration / 8.0)
            "days" -> salary * duration
            "weeks" -> salary * duration * 5 // 5-day work week
            "months" -> salary * duration * 22 // 22 working days per month
            else -> 0.0
        }
    }

    private fun calculateWeeklyTotal(salary: Double, duration: Int, durationUnit: String): Double {
        return when (durationUnit.lowercase()) {
            "hours" -> salary * (duration / 40.0)
            "days" -> salary * (duration / 5.0)
            "weeks" -> salary * duration
            "months" -> salary * duration * 4.33 // Average weeks per month
            else -> 0.0
        }
    }

    private fun calculateMonthlyTotal(salary: Double, duration: Int, durationUnit: String): Double {
        return when (durationUnit.lowercase()) {
            "hours" -> salary * (duration / 160.0)
            "days" -> salary * (duration / 22.0)
            "weeks" -> salary * (duration / 4.33)
            "months" -> salary * duration
            else -> 0.0
        }
    }

// I will continue with the job creation and state management implementation...
// Job Creation and Management
fun createJob() {
    viewModelScope.launch {
        try {
            validateAllFields()
            val currentState = getState()

            if (currentState.validationErrors.isNotEmpty()) {
                // Create a single Map with all validation errors
                val validationError = CreateJobEvent.ValidationError(
                    errors = currentState.validationErrors.mapValues { it.value }
                )
                emitEvent(validationError)
                return@launch
            }

            setState { copy(isLoading = true) }
            val job = createJobModel()

            createJobUseCase.invoke(job).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        // No need for casting since result.data is already a Job
                        handleJobCreationSuccess(result.data)
                    }
                    is ApiResult.Error -> {
                        handleError(AppError.UnexpectedError(
                            message = "Failed to create job",
                            cause = result.error.cause
                        ))
                        setState { copy(isLoading = false) }
                    }
                    is ApiResult.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                }
            }
        } catch (e: Exception) {
            handleError(AppError.UnexpectedError(
                message = "Failed to create job",
                cause = e
            ))
            setState { copy(isLoading = false) }
        }
    }
}
    // Add this type alias at the top of your file if needed
    private fun createJobModel(): Job {
        return with(getState()) {
            Job(
                id = "",  // Will be assigned by backend
                title = title.trim(),
                description = description.trim(),
                employerId = getSavedState("userId", ""),
                location = Location(
                    state = state,
                    district = district,
                    latitude = latitude,
                    longitude = longitude,
                    address = null,
                    pinCode = null
                ),
                salary = salary.toDoubleOrNull() ?: 0.0,
                salaryUnit = salaryUnit,
                workDuration = workDuration.toIntOrNull() ?: 0,
                workDurationUnit = workDurationUnit,
                status = "OPEN",
                createdAt = "",  // Will be assigned by backend
                updatedAt = "",  // Will be assigned by backend
                lastModified = "",  // Will be assigned by backend
                company = "",  // Add company name if available or leave empty
                applicationDeadline = null  // Add deadline if needed
            )
        }
    }

    private fun handleJobCreationSuccess(job: Job) {
        viewModelScope.launch {
            setState {
                copy(
                    isLoading = false,
                    isSuccess = true,
                    errorMessage = null
                )
            }
            // Clear all saved states
            clearAllSavedState()

            draftId?.let { id ->
                jobDraftRepository.deleteDraft(id)
            }
            emitEvent(CreateJobEvent.JobCreated(job.id))
        }
    }
    // Draft Management
    private fun saveDraft() {
        viewModelScope.launch {
            try {
                val draft = createDraftModel()
                jobDraftRepository.saveDraft(draft)
                setState {
                    copy(
                        isDraft = true,
                        lastSavedAt = System.currentTimeMillis(),
                        isDirty = false
                    )
                }
            } catch (e: Exception) {
                handleError(AppError.Database(
                    message = "Failed to save draft",
                    cause = e
                ))
            }
        }
    }

    fun deleteDraft() {
        viewModelScope.launch {
            try {
                draftId?.let { id ->
                    jobDraftRepository.deleteDraft(id)
                    setState { copy(isDraft = false) }
                    emitEvent(CreateJobEvent.DraftDeleted)
                }
            } catch (e: Exception) {
                handleError(AppError.Database(
                    message = "Failed to delete draft",
                    cause = e
                ))
            }
        }
    }

    private fun createDraftModel(): JobDraft {
        return with(getState()) {
            JobDraft(
                id = draftId ?: UUID.randomUUID().toString(),
                title = title,
                description = description,
                salary = salary.toDoubleOrNull() ?: 0.0,
                salaryUnit = salaryUnit,
                workDuration = workDuration.toIntOrNull() ?: 0,
                workDurationUnit = workDurationUnit,
                location = Location(
                    state = state,
                    district = district,
                    latitude = latitude,
                    longitude = longitude,
                    address = null,
                    pinCode = null
                ),
                lastModified = System.currentTimeMillis()
            )
        }
    }

    // State Management Utilities
    private fun updateValidationState(field: String, result: ValidationResult) {
        setState {
            val newErrors = validationErrors.toMutableMap()
            when (result) {
                is ValidationResult.Invalid -> {
                    newErrors[field] = UiValidationError(
                        message = result.message,
                        errorType = convertToFieldErrorType(result.message),
                        field = field
                    )
                }
                ValidationResult.Valid -> newErrors.remove(field)
            }
            copy(validationErrors = newErrors)
        }
    }

    // Helper function to determine FieldErrorType based on error message
    private fun convertToFieldErrorType(message: String): FieldErrorType {
        return when {
            message.contains("required", ignoreCase = true) -> FieldErrorType.REQUIRED
            message.contains("too short", ignoreCase = true) -> FieldErrorType.TOO_SHORT
            message.contains("too long", ignoreCase = true) -> FieldErrorType.TOO_LONG
            message.contains("pattern", ignoreCase = true) -> FieldErrorType.PATTERN_MISMATCH
            message.contains("invalid", ignoreCase = true) -> FieldErrorType.INVALID_VALUE
            message.contains("format", ignoreCase = true) -> FieldErrorType.INVALID_FORMAT
            message.contains("range", ignoreCase = true) -> FieldErrorType.OUT_OF_RANGE
            else -> FieldErrorType.INVALID_VALUE
        }
    }

    private fun triggerAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY)
            if (getState().isDirty && getState().validationErrors.isEmpty()) {
                saveDraft()
            }
        }
    }
    // Preview Management Functions
    fun togglePreview() {
        viewModelScope.launch {
            val currentPreviewMode = getState().isPreviewMode
            setState { copy(isPreviewMode = !currentPreviewMode) }
            if (!currentPreviewMode) {
                validateAllFields()
            }
        }
    }

    fun exitPreview() {
        viewModelScope.launch {
            setState { copy(isPreviewMode = false) }
            validateAllFields()
        }
    }

    // Location Management Functions
    private suspend fun loadLocationData() {
        try {
            setState { copy(isLoadingLocation = true) }
            locationRepository.getStates().collect { result ->
                // LocationRepository returns ApiResult, not Result
                when (result) {
                    is ApiResult.Success -> {
                        setState {
                            copy(
                                availableStates = result.data,
                                isLoadingLocation = false
                            )
                        }
                        // Load districts if state is already selected
                        getState().state?.let { selectedState ->
                            loadDistricts(selectedState)
                        }
                    }
                    is ApiResult.Error -> {
                        handleError(AppError.Network(
                            message = "Failed to load states",
                            cause = result.error.cause
                        ))
                        setState { copy(isLoadingLocation = false) }
                    }
                    is ApiResult.Loading -> {
                        setState { copy(isLoadingLocation = true) }
                    }
                }
            }
        } catch (e: Exception) {
            handleError(AppError.Network(
                message = "Failed to load location data",
                cause = e
            ))
            setState { copy(isLoadingLocation = false) }
        }
    }

    private suspend fun loadDistricts(state: String) {
        try {
            setState { copy(isLoadingLocation = true) }
            locationRepository.getDistricts(state).collect { result ->
                // LocationRepository returns ApiResult, not Result
                when (result) {
                    is ApiResult.Success -> {
                        setState {
                            copy(
                                availableDistricts = result.data,
                                isLoadingLocation = false
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        handleError(AppError.Network(
                            message = "Failed to load districts",
                            cause = result.error.cause
                        ))
                        setState { copy(isLoadingLocation = false) }
                    }
                    is ApiResult.Loading -> {
                        setState { copy(isLoadingLocation = true) }
                    }
                }
            }
        } catch (e: Exception) {
            handleError(AppError.Network(
                message = "Failed to load district data",
                cause = e
            ))
            setState { copy(isLoadingLocation = false) }
        }
    }
    // Additional Utility Functions
    private fun isLocationDataValid(): Boolean {
        return with(getState()) {
            state.isNotBlank() &&
                    district.isNotBlank() &&
                    availableStates.contains(state) &&
                    availableDistricts.contains(district)
        }
    }

    private fun resetLocationData() {
        setState {
            copy(
                state = "",
                district = "",
                availableDistricts = emptyList(),
                latitude = null,
                longitude = null
            )
        }
    }

    fun refreshLocationData() {
        viewModelScope.launch {
            loadLocationData()
        }
    }

    // Lifecycle Management
    override fun clearAllSavedState() {
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

    override fun onCleared() {
        autosaveJob?.cancel()
        viewModelScope.launch {
            if (getState().isDraft) {
                saveDraft()
            }
        }
        clearAllSavedState()
        super.onCleared()
    }

    companion object {
        private const val AUTOSAVE_DELAY = 1000L
        private const val MIN_TITLE_LENGTH = 5
        private const val MAX_TITLE_LENGTH = 100
        private const val MIN_DESCRIPTION_LENGTH = 20
        private const val MAX_DESCRIPTION_LENGTH = 1000
        private const val MAX_SALARY = 1000000.0
        private const val MIN_DURATION = 1
        private const val MAX_DURATION = 365

        private val VALID_SALARY_UNITS = setOf("hourly", "daily", "weekly", "monthly")
        private val VALID_DURATION_UNITS = setOf("hours", "days", "weeks", "months")

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
}
