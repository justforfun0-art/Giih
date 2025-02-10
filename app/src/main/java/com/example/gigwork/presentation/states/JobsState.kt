package com.example.gigwork.presentation.states

import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.presentation.screens.common.UiState

data class JobsState(
    // Data
    val jobs: List<Job> = emptyList(),
    val userProfile: UserProfile? = null,
    val totalJobs: Int = 0,
    val filteredJobsCount: Int = 0,

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

    // Error States
    override val errorMessage: ErrorMessage? = null,
    val searchError: String? = null,
    val profileError: String? = null,

    // Network States
    val isNetworkAvailable: Boolean = true,
    val hasNetworkError: Boolean = false,
    val lastNetworkCheck: Long = 0,

    // Operation States
    val lastSuccessfulOperation: Long = 0,
    val lastFailedOperation: Long = 0,
    val operationRetryCount: Int = 0
) : UiState

sealed class JobsEvent {
    data class ShowSnackbar(val message: String) : JobsEvent()
    data class NavigateToJobDetail(val jobId: String) : JobsEvent()
    object ScrollToTop : JobsEvent()
    data class NetworkStatusChanged(
        val isAvailable: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    ) : JobsEvent()
    data class ValidationError(val errors: Map<String, String>) : JobsEvent()
    object RefreshComplete : JobsEvent()
    data class SearchComplete(val resultCount: Int) : JobsEvent()
}

data class JobsFilterData(
    val searchQuery: String,
    val state: String?,
    val district: String?,
    val minSalary: Double?,
    val maxSalary: Double?,
    val sortOrder: String
)