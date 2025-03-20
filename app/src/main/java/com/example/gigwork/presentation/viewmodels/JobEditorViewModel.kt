package com.example.gigwork.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.models.Location
import com.example.gigwork.domain.usecase.job.JobPublicationCoordinator
import com.example.gigwork.presentation.states.JobDraft
import com.example.gigwork.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class JobEditorViewModel @Inject constructor(
    private val jobPublicationCoordinator: JobPublicationCoordinator,
    private val savedStateHandle: SavedStateHandle,
    private val logger: Logger
) : ViewModel() {

    companion object {
        private const val TAG = "JobEditorViewModel"
        private const val KEY_DRAFT_ID = "draftId"
        private const val KEY_JOB_ID = "jobId"
        private const val KEY_IS_EDITING = "isEditing"
    }

    private val _uiState = MutableStateFlow(JobEditorUiState())
    val uiState: StateFlow<JobEditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<JobEditorEvent>()
    val events = _events.asSharedFlow()

    private var currentDraftId: String? = savedStateHandle[KEY_DRAFT_ID]
    private var currentJobId: String? = savedStateHandle[KEY_JOB_ID]
    private var isEditing: Boolean = savedStateHandle[KEY_IS_EDITING] ?: false

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        currentDraftId?.let { draftId ->
            loadDraft(draftId)
        }
        currentJobId?.let { jobId ->
            if (isEditing) {
                createDraftFromJob(jobId)
            }
        }
    }

    fun createNewDraft() {
        viewModelScope.launch {
            val newDraft = JobDraft(
                id = UUID.randomUUID().toString(),
                title = "",
                description = "",
                salary = 0.0,
                salaryUnit = "monthly",
                workDuration = 0,
                workDurationUnit = "days",
                location = Location(
                    latitude = 0.0,
                    longitude = 0.0,
                    address = null,
                    pinCode = null,
                    state = "",
                    district = ""
                ),
                lastModified = System.currentTimeMillis()
            )

            currentDraftId = newDraft.id
            savedStateHandle[KEY_DRAFT_ID] = newDraft.id
            _uiState.update { it.copy(currentDraft = newDraft) }
        }
    }

    fun publishDraft() {
        viewModelScope.launch {
            val draftId = currentDraftId ?: return@launch
            _uiState.update { it.copy(isLoading = true) }

            jobPublicationCoordinator.publishDraft(draftId)
                .onEach { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    publishedJob = result.data,
                                    currentDraft = null
                                )
                            }
                            currentDraftId = null
                            savedStateHandle[KEY_DRAFT_ID] = null
                            _events.emit(JobEditorEvent.PublishSuccess(result.data.id))
                            logger.d(TAG, "Draft published successfully", mapOf(
                                "draftId" to draftId,
                                "jobId" to result.data.id
                            ))
                        }
                        is ApiResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = result.error.getUserMessage()
                                )
                            }
                            _events.emit(JobEditorEvent.PublishError(result.error.getUserMessage()))
                            logger.e(TAG, "Failed to publish draft", result.error.cause, mapOf(
                                "errorType" to result.error.javaClass.simpleName,
                                "errorMessage" to result.error.message,
                                "draftId" to draftId
                            ))
                        }
                        is ApiResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "An unexpected error occurred"
                        )
                    }
                    _events.emit(JobEditorEvent.PublishError(throwable.message ?: "Publication failed"))
                    logger.e(TAG, "Error publishing draft", throwable)
                }
                .collect()
        }
    }

    fun createDraftFromJob(jobId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            jobPublicationCoordinator.createDraftFromJob(jobId)
                .onEach { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    currentDraft = result.data
                                )
                            }
                            currentDraftId = result.data.id
                            savedStateHandle[KEY_DRAFT_ID] = result.data.id
                            savedStateHandle[KEY_JOB_ID] = jobId
                            savedStateHandle[KEY_IS_EDITING] = true
                            _events.emit(JobEditorEvent.DraftCreated(result.data.id))
                            logger.d(TAG, "Draft created from job", mapOf(
                                "jobId" to jobId,
                                "draftId" to result.data.id
                            ))
                        }
                        is ApiResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = result.error.getUserMessage()
                                )
                            }
                            _events.emit(JobEditorEvent.DraftCreationError(result.error.getUserMessage()))
                            logger.e(TAG, "Failed to create draft from job", result.error.cause, mapOf(
                                "errorType" to result.error.javaClass.simpleName,
                                "errorMessage" to result.error.message,
                                "jobId" to jobId
                            ))
                        }
                        is ApiResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "An unexpected error occurred"
                        )
                    }
                    _events.emit(JobEditorEvent.DraftCreationError("Draft creation failed"))
                    logger.e(TAG, "Error creating draft from job", throwable)
                }
                .collect()
        }
    }
    fun updateJobFromDraft() {
        viewModelScope.launch {
            val jobId = currentJobId ?: return@launch
            val draftId = currentDraftId ?: return@launch
            _uiState.update { it.copy(isLoading = true) }

            jobPublicationCoordinator.updateJobFromDraft(jobId, draftId)
                .onEach { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    publishedJob = result.data,
                                    currentDraft = null
                                )
                            }
                            currentDraftId = null
                            savedStateHandle[KEY_DRAFT_ID] = null
                            savedStateHandle[KEY_IS_EDITING] = false
                            _events.emit(JobEditorEvent.UpdateSuccess(result.data.id))
                            logger.d(TAG, "Job updated from draft", mapOf(
                                "jobId" to jobId,
                                "draftId" to draftId
                            ))
                        }
                        is ApiResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = result.error.getUserMessage()
                                )
                            }
                            _events.emit(JobEditorEvent.UpdateError(result.error.getUserMessage()))
                            logger.e(TAG, "Failed to update job from draft", result.error.cause, mapOf(
                                "errorType" to result.error.javaClass.simpleName,
                                "errorMessage" to result.error.message
                            ))
                        }
                        is ApiResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "An unexpected error occurred"
                        )
                    }
                    _events.emit(JobEditorEvent.UpdateError("Update failed"))
                    logger.e(TAG, "Error updating job from draft", throwable)
                }
                .collect()
        }
    }
    private fun loadDraft(draftId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Implementation of draft loading logic
                // This would use the JobDraftRepository to load the draft
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load draft"
                    )
                }
                logger.e(TAG, "Error loading draft", e)
            }
        }
    }

    fun updateDraftField(field: String, value: Any) {
        viewModelScope.launch {
            val currentDraft = _uiState.value.currentDraft ?: return@launch
            val updatedDraft = when (field) {
                "title" -> currentDraft.copy(title = value as String)
                "description" -> currentDraft.copy(description = value as String)
                "salary" -> currentDraft.copy(salary = (value as String).toDoubleOrNull() ?: 0.0)
                "salaryUnit" -> currentDraft.copy(salaryUnit = value as String)
                "workDuration" -> currentDraft.copy(workDuration = (value as String).toIntOrNull() ?: 0)
                "workDurationUnit" -> currentDraft.copy(workDurationUnit = value as String)
                else -> currentDraft
            }

            _uiState.update { it.copy(currentDraft = updatedDraft) }
            // Implement auto-save logic here if needed
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        // Implement any cleanup logic here
    }
}

sealed class JobEditorEvent {
    data class PublishSuccess(val jobId: String) : JobEditorEvent()
    data class PublishError(val message: String) : JobEditorEvent()
    data class DraftCreated(val draftId: String) : JobEditorEvent()
    data class DraftCreationError(val message: String) : JobEditorEvent()
    data class UpdateSuccess(val jobId: String) : JobEditorEvent()
    data class UpdateError(val message: String) : JobEditorEvent()
    data class ValidationError(val errors: Map<String, String>) : JobEditorEvent()
    object NavigateBack : JobEditorEvent()
}

data class JobEditorUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentDraft: JobDraft? = null,
    val publishedJob: Job? = null,
    val isDirty: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap(),
    val isEditMode: Boolean = false
) {
    val canPublish: Boolean
        get() = currentDraft != null && !isLoading && validationErrors.isEmpty()

    val canUpdate: Boolean
        get() = currentDraft != null && !isLoading && validationErrors.isEmpty() && isEditMode
}