package com.example.gigwork.presentation.states

import com.example.gigwork.core.error.model.ErrorAction
import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.presentation.base.UiEvent
import com.example.gigwork.presentation.base.UiState

data class JobsState(
    val validationErrors: List<String> = emptyList(),
    val successMessage: String? = null,
    val selectedJobId: String? = null,
    // Data
    val jobs: List<Job> = emptyList(),
    val userProfile: UserProfile? = null,
    val totalJobs: Int = 0,
    val filteredJobsCount: Int = 0,

    val jobMetrics: JobMetrics? = null,
    val job: Job? = null,

    // Pagination
    val currentPage: Int = 1,
    val isLastPage: Boolean = false,
    val totalPages: Int = 1,

    // Filters
    val searchQuery: String = "",
    val selectedState: String? = null,
    val selectedDistrict: String? = null,
    val minSalary: Double? = null,
    val maxSalary: Double? = null,
    val sortOrder: String = "newest",
    val activeFiltersCount: Int = 0,

    // Loading States
    override val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSearching: Boolean = false,
    val isLoadingNextPage: Boolean = false,
    val isLoadingProfile: Boolean = false,
    val isApplying: Boolean = false,

    val appliedJobIds: Set<String> = emptySet(),

    // Error States
    override val errorMessage: ErrorMessage? = null,
    val searchError: String? = null,
    val profileError: String? = null,
    val isUpdating: Boolean = false,

    // Network States
    val isNetworkAvailable: Boolean = true,
    val hasNetworkError: Boolean = false,
    val lastNetworkCheck: Long = 0,

    // Operation States
    val lastSuccessfulOperation: Long = 0,
    val lastFailedOperation: Long = 0,
    val operationRetryCount: Int = 0
) : UiState<JobsState> {

    override fun copy(
        isLoading: Boolean,
        errorMessage: ErrorMessage?
    ): JobsState {
        // Use a different name for the data class copy method
        return super_copy(
            isLoading = isLoading,
            errorMessage = errorMessage
        )
    }

    // This is a helper method to disambiguate from the 'copy' method
    private fun super_copy(
        isLoading: Boolean,
        errorMessage: ErrorMessage?
    ): JobsState {
        return JobsState(
            // Set the parameters that are being changed
            isLoading = isLoading,
            errorMessage = errorMessage,

            // Copy all other properties from the current instance
            validationErrors = this.validationErrors,
            successMessage = this.successMessage,
            selectedJobId = this.selectedJobId,
            jobs = this.jobs,
            userProfile = this.userProfile,
            totalJobs = this.totalJobs,
            filteredJobsCount = this.filteredJobsCount,
            jobMetrics = this.jobMetrics,
            job = this.job,
            currentPage = this.currentPage,
            isLastPage = this.isLastPage,
            totalPages = this.totalPages,
            searchQuery = this.searchQuery,
            selectedState = this.selectedState,
            selectedDistrict = this.selectedDistrict,
            minSalary = this.minSalary,
            maxSalary = this.maxSalary,
            sortOrder = this.sortOrder,
            activeFiltersCount = this.activeFiltersCount,
            isRefreshing = this.isRefreshing,
            isSearching = this.isSearching,
            isLoadingNextPage = this.isLoadingNextPage,
            isLoadingProfile = this.isLoadingProfile,
            isApplying = this.isApplying,
            appliedJobIds = this.appliedJobIds,
            searchError = this.searchError,
            profileError = this.profileError,
            isUpdating = this.isUpdating,
            isNetworkAvailable = this.isNetworkAvailable,
            hasNetworkError = this.hasNetworkError,
            lastNetworkCheck = this.lastNetworkCheck,
            lastSuccessfulOperation = this.lastSuccessfulOperation,
            lastFailedOperation = this.lastFailedOperation,
            operationRetryCount = this.operationRetryCount
        )
    }
}
sealed class JobsEvent : UiEvent {
    data class ShowSnackbar(val message: String) : JobsEvent()
    data class NavigateToJobDetail(val jobId: String) : JobsEvent()
    object ScrollToTop : JobsEvent()
    object DismissError : JobsEvent()
    data class SearchQueryChanged(val query: String) : JobsEvent()
    data class HandleError(val action: ErrorAction) : JobsEvent()
    data class NetworkStatusChanged(
        val isAvailable: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    ) : JobsEvent()
    data class ValidationError(val errors: Map<String, String>) : JobsEvent()
    object RefreshComplete : JobsEvent()
    data class SearchComplete(val resultCount: Int) : JobsEvent()
    object Refresh : JobsEvent()
}

data class JobsFilterData(
    val searchQuery: String = "",
    val state: String? = null,
    val district: String? = null,
    val minSalary: Double? = null,
    val maxSalary: Double? = null,
    val sortOrder: String = "newest"
)