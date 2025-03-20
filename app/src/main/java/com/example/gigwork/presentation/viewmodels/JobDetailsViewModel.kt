package com.example.gigwork.presentation.viewmodels


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gigwork.core.error.handler.GlobalErrorHandler
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.error.model.ErrorAction
import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.repository.JobRepository
import com.example.gigwork.domain.repository.JobApplicationRepository
import com.example.gigwork.domain.usecase.job.GetJobDetailsUseCase
import com.example.gigwork.domain.usecase.job.ApplyForJobUseCase
import com.example.gigwork.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobDetailsState(
    val job: Job? = null,
    val isLoading: Boolean = false,
    val isApplying: Boolean = false,
    val hasApplied: Boolean = false,
    val errorMessage: ErrorMessage? = null
)

@HiltViewModel
class JobDetailsViewModel @Inject constructor(
    private val getJobDetailsUseCase: GetJobDetailsUseCase,
    private val applyForJobUseCase: ApplyForJobUseCase,
    private val errorHandler: GlobalErrorHandler,
    private val logger: Logger,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobDetailsState())
    val uiState: StateFlow<JobDetailsState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "JobDetailsViewModel"
    }

    fun loadJobDetails(jobId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                getJobDetailsUseCase(jobId).collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                job = result.data,
                                isLoading = false,
                                errorMessage = null
                            )
                            checkApplicationStatus(jobId)
                        }
                        is ApiResult.Error -> handleError(result.error)
                        is ApiResult.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                }
            } catch (e: Exception) {
                handleError(
                    AppError.UnexpectedError(
                        message = "Failed to load job details",
                        cause = e
                    )
                )
            }
        }
    }

    private fun checkApplicationStatus(jobId: String) {
        viewModelScope.launch {
            try {
                applyForJobUseCase.hasApplied(jobId).collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                hasApplied = result.data
                            )
                        }
                        is ApiResult.Error -> logger.e(
                            tag = TAG,
                            message = "Failed to check application status",
                            throwable = result.error
                        )
                        is ApiResult.Loading -> Unit
                    }
                }
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Failed to check application status",
                    throwable = e
                )
            }
        }
    }

    fun applyForJob() {
        val jobId = _uiState.value.job?.id ?: return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isApplying = true)

                applyForJobUseCase(jobId).collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                hasApplied = true,
                                isApplying = false,
                                errorMessage = null
                            )
                        }
                        is ApiResult.Error -> handleError(result.error)
                        is ApiResult.Loading -> _uiState.value = _uiState.value.copy(isApplying = true)
                    }
                }
            } catch (e: Exception) {
                handleError(
                    AppError.UnexpectedError(
                        message = "Failed to apply for job",
                        cause = e
                    )
                )
            } finally {
                _uiState.value = _uiState.value.copy(isApplying = false)
            }
        }
    }

    private fun handleError(error: AppError) {
        logger.e(
            tag = TAG,
            message = "Error in JobDetailsViewModel: ${error.message}",
            throwable = error,
            additionalData = mapOf(
                "jobId" to (_uiState.value.job?.id ?: "unknown"),
                "error_type" to error::class.simpleName
            )
        )

        val errorMessage = errorHandler.handleCoreError(error)
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isApplying = false,
            errorMessage = errorMessage
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun handleErrorAction(action: ErrorAction) {
        when (action) {
            is ErrorAction.Retry -> {
                _uiState.value.job?.id?.let { loadJobDetails(it) }
            }
            else -> clearError()
        }
    }
}