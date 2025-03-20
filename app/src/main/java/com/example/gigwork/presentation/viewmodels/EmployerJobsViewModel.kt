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
import com.example.gigwork.domain.usecase.job.GetEmployerJobsUseCase
import com.example.gigwork.domain.usecase.job.UpdateJobStatusUseCase
import com.example.gigwork.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EmployerJobsState(
    val jobs: List<Job> = emptyList(),
    val searchQuery: String = "",
    val showFilters: Boolean = false,
    val selectedState: String? = null,
    val selectedDistrict: String? = null,
    val minSalary: Double? = null,
    val maxSalary: Double? = null,
    val isLoading: Boolean = false,
    val errorMessage: ErrorMessage? = null
)



@HiltViewModel
class EmployerJobsViewModel @Inject constructor(
    private val getEmployerJobsUseCase: GetEmployerJobsUseCase,
    private val updateJobStatusUseCase: UpdateJobStatusUseCase,
    private val errorHandler: GlobalErrorHandler,
    private val logger: Logger,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "EmployerJobsViewModel"
        private const val KEY_SEARCH_QUERY = "searchQuery"
        private const val KEY_SHOW_FILTERS = "showFilters"
        private const val SEARCH_DEBOUNCE_MS = 300L
    }

    private val _uiState = MutableStateFlow(EmployerJobsState())
    val uiState: StateFlow<EmployerJobsState> = _uiState.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null


    private val searchQuery = savedStateHandle.getStateFlow(KEY_SEARCH_QUERY, "")
    private val showFilters = savedStateHandle.getStateFlow(KEY_SHOW_FILTERS, false)

    init {
        initializeState()
        observeSearchQuery()
    }

    private fun initializeState() {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    searchQuery = searchQuery.value,
                    showFilters = showFilters.value
                )
            }
            loadJobs()
        }
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            searchQuery
                .debounce(SEARCH_DEBOUNCE_MS)
                .collect { query ->
                    _uiState.update { it.copy(searchQuery = query) }
                    loadJobs()
                }
        }
    }

    fun loadJobs() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Create JobFilters object
                val jobFilters = JobFilters(
                    status = null,  // Add status if needed
                    minSalary = _uiState.value.minSalary,
                    maxSalary = _uiState.value.maxSalary,
                    location = if (_uiState.value.selectedState != null && _uiState.value.selectedDistrict != null) {
                        "${_uiState.value.selectedState},${_uiState.value.selectedDistrict}"
                    } else null
                )

                getEmployerJobsUseCase(
                    GetEmployerJobsUseCase.Params(
                        employerId = getUserId(),
                        searchQuery = _uiState.value.searchQuery,
                        filters = jobFilters,  // Pass the filters object here
                        page = 1,
                        pageSize = 20
                    )
                ).collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    jobs = result.data,
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
                        message = "Failed to load jobs",
                        cause = e
                    )
                )
            }
        }
    }

    fun updateJobStatus(jobId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                updateJobStatusUseCase(jobId, newStatus).collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            loadJobs() // Reload jobs to reflect the update
                        }
                        is ApiResult.Error -> handleError(result.error)
                        is ApiResult.Loading -> Unit // Status updates are quick, no need to show loading
                    }
                }
            } catch (e: Exception) {
                handleError(
                    AppError.UnexpectedError(
                        message = "Failed to update job status",
                        cause = e
                    )
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
    }

    fun toggleFilters() {
        val newShowFilters = !_uiState.value.showFilters
        savedStateHandle[KEY_SHOW_FILTERS] = newShowFilters
        _uiState.update { it.copy(showFilters = newShowFilters) }
    }

    fun updateFilters(state: String?, district: String?, minSalary: Double?, maxSalary: Double?, duration: String?) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedState = state,
                selectedDistrict = district,
                minSalary = minSalary,
                maxSalary = maxSalary
            )
        }
        loadJobs()
    }

    fun clearFilters() {
        _uiState.update { currentState ->
            currentState.copy(
                selectedState = null,
                selectedDistrict = null,
                minSalary = null,
                maxSalary = null
            )
        }
        loadJobs()
    }

    private fun getUserId(): String {
        return savedStateHandle.get<String>("userId") ?: ""
    }

    private fun handleError(error: AppError) {
        logger.e(
            tag = TAG,
            message = "Error in EmployerJobsViewModel: ${error.message}",
            throwable = error,
            additionalData = mapOf(
                "search_query" to _uiState.value.searchQuery,
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun handleErrorAction(action: ErrorAction) {
        when (action) {
            is ErrorAction.Retry -> loadJobs()
            else -> clearError()
        }
    }
    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()  // Correct way to cancel the job
    }
}