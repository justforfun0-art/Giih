package com.example.gigwork.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gigwork.core.error.handler.GlobalErrorHandler
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.error.model.ErrorAction
import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.DashboardMetrics
import com.example.gigwork.domain.usecase.job.GetEmployerDashboardMetricsUseCase
import com.example.gigwork.domain.usecase.user.GetCurrentUserUseCase
import com.example.gigwork.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EmployerDashboardState(
    val metrics: DashboardMetrics? = null,
    val employerName: String = "",  // Add this property
    val isLoading: Boolean = false,
    val errorMessage: ErrorMessage? = null,
    val userId: String? = null
)

sealed class EmployerDashboardEvent {
    object NavigateToCreateJob : EmployerDashboardEvent()
    object NavigateToJobs : EmployerDashboardEvent()
    data class NavigateToProfile(val userId: String) : EmployerDashboardEvent()
}

@HiltViewModel
class EmployerDashboardViewModel @Inject constructor(
    private val getDashboardMetricsUseCase: GetEmployerDashboardMetricsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val errorHandler: GlobalErrorHandler,
    private val logger: Logger
) : ViewModel() {

    companion object {
        private const val TAG = "EmployerDashboardViewModel"
    }

    private val _uiState = MutableStateFlow(EmployerDashboardState())
    val uiState: StateFlow<EmployerDashboardState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EmployerDashboardEvent>()
    val events: SharedFlow<EmployerDashboardEvent> = _events.asSharedFlow()

    init {
        getCurrentUser()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                getDashboardMetricsUseCase().collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    metrics = result.data,
                                    isLoading = false,
                                    errorMessage = null
                                )
                            }
                        }
                        is ApiResult.Error -> handleError(result.error)
                        is ApiResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    }
                }
            } catch (e: Exception) {
                handleError(
                    AppError.UnexpectedError(
                        message = "Failed to load dashboard data",
                        cause = e
                    )
                )
            }
        }
    }

    private fun getCurrentUser() {
        viewModelScope.launch {
            try {
                getCurrentUserUseCase().collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            _uiState.update {
                                it.copy(userId = result.data.id)
                            }
                        }
                        is ApiResult.Error -> {
                            logger.e(
                                tag = TAG,
                                message = "Error getting current user",
                                throwable = result.error
                            )
                        }
                        is ApiResult.Loading -> Unit
                    }
                }
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Failed to get current user",
                    throwable = e
                )
            }
        }
    }

    fun navigateToCreateJob() {
        viewModelScope.launch {
            _events.emit(EmployerDashboardEvent.NavigateToCreateJob)
        }
    }

    fun navigateToJobs() {
        viewModelScope.launch {
            _events.emit(EmployerDashboardEvent.NavigateToJobs)
        }
    }

    fun navigateToProfile() {
        viewModelScope.launch {
            val userId = _uiState.value.userId
            if (userId != null) {
                _events.emit(EmployerDashboardEvent.NavigateToProfile(userId))
            } else {
                handleError(
                    AppError.BusinessError(
                        message = "User ID not available"
                    )
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun handleErrorAction(action: ErrorAction) {
        when (action) {
            is ErrorAction.Retry -> loadDashboardData()
            else -> clearError()
        }
    }

    private fun handleError(error: AppError) {
        logger.e(
            tag = TAG,
            message = "Error in EmployerDashboardViewModel: ${error.message}",
            throwable = error,
            additionalData = mapOf(
                "error_type" to error::class.simpleName
            )
        )

        val errorMessage = errorHandler.handleCoreError(error)
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = errorMessage
            )
        }
    }
}