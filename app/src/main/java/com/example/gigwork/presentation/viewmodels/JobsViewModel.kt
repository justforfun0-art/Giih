package com.example.gigwork.presentation.viewmodels

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.gigwork.core.error.handler.GlobalErrorHandler
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.repository.*
import com.example.gigwork.domain.usecase.job.GetJobsUseCase
import com.example.gigwork.domain.usecase.job.GetUserProfileUseCase
import com.example.gigwork.presentation.base.BaseErrorViewModel
import com.example.gigwork.presentation.states.JobsState
import com.example.gigwork.presentation.states.JobsEvent
import com.example.gigwork.presentation.states.JobsFilterData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.gigwork.presentation.states.JobMetrics

@HiltViewModel
class JobsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val getJobsUseCase: GetJobsUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val jobRepository: JobRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val applicationRepository: JobApplicationRepository,
    private val userLocationRepository: UserLocationRepository,
    private val employerRatingRepository: EmployerRatingRepository,
    private val jobStatisticsRepository: JobStatisticsRepository,
    private val fileRepository: FileRepository,
    private val locationManager: LocationManager,
    errorHandler: GlobalErrorHandler,  // Changed from GlobalErrorHandler to ErrorHandler
     logger: com.example.gigwork.presentation.base.Logger,  // Using fully qualified Logger interface
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BaseErrorViewModel<JobsState, JobsEvent>(savedStateHandle, errorHandler, logger) {

    companion object {
        // View Model identification
        private const val TAG = "JobsViewModel"

        // Pagination constants
        private const val PAGE_SIZE = 20
        private const val INITIAL_PAGE = 1

        // Location constants
        private const val DEFAULT_RADIUS = 50.0 // km

        // File handling constants
        private const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB

        // Application constants
        private const val APPLICATION_DEADLINE_HOURS = 24
    }

    private val _filterData = MutableStateFlow(JobsFilterData())
    private val filterData: StateFlow<JobsFilterData> = _filterData.asStateFlow()

    private val _networkAvailable = MutableStateFlow(true)
    private var currentUserId: String? = null

    private val userId: String = savedStateHandle.get<String>("userId") ?: ""

    init {
        viewModelScope.launch {
            // If userId is empty, try to get it from the repository
            currentUserId = if (userId.isNotEmpty()) {
                userId
            } else {
                loadCurrentUserId() ?: ""
            }

            initializeData()
            observeNetworkChanges()
        }
    }

    private fun initializeData() {
        viewModelScope.launch {
            try {
                currentUserId = userId
                loadUserProfile()
                loadJobs(forceRefresh = true)
            }catch (e: Exception) {
                throw convertToPresentationError(e)

            }
        }
    }
    private suspend fun loadCurrentUserId(): String? {
        return userRepository.getUserProfile().first().let { result ->
            when (result) {
                is ApiResult.Success -> result.data.id
                is ApiResult.Error -> {
                    logDebug(TAG, "Failed to load user ID: ${result.error.message}")
                    null
                }
                is ApiResult.Loading -> null
            }
        }
    }

    override fun createInitialState() = JobsState()


    private fun loadUserProfile() = safeLaunch {
        setState { copy(isLoadingProfile = true) }
        getUserProfileUseCase().collect { result ->
            when (result) {
                is ApiResult.Success -> {
                    setState {
                        copy(
                            userProfile = result.data,
                            isLoadingProfile = false,
                            profileError = null
                        )
                    }
                }

                is ApiResult.Error -> {
                    setState {
                        copy(
                            isLoadingProfile = false,
                            profileError = result.error.message
                        )
                    }
                }

                is ApiResult.Loading -> setState { copy(isLoadingProfile = true) }
            }
        }
    }

    private fun convertToPresentationError(error: Exception): com.example.gigwork.presentation.base.AppError {
        return when (error) {
            is com.example.gigwork.core.error.model.AppError -> {
                when (error) {
                    is com.example.gigwork.core.error.model.AppError.NetworkError ->
                        com.example.gigwork.presentation.base.AppError.Network(
                            message = error.message,
                            cause = error.cause
                        )
                    is com.example.gigwork.core.error.model.AppError.ValidationError ->
                        com.example.gigwork.presentation.base.AppError.Validation(
                            message = error.message,
                            errors = mapOf("validation" to error.message)
                        )
                    is com.example.gigwork.core.error.model.AppError.DatabaseError ->
                        com.example.gigwork.presentation.base.AppError.Database(
                            message = error.message,
                            cause = error.cause
                        )
                    else -> com.example.gigwork.presentation.base.AppError.UnexpectedError(
                        message = error.message,
                        cause = error.cause
                    )
                }
            }
            else -> com.example.gigwork.presentation.base.AppError.UnexpectedError(
                message = error.message ?: "An unexpected error occurred",
                cause = error
            )
        }
    }
    fun onEvent(event: JobsEvent) {
        viewModelScope.launch {
            when (event) {
                is JobsEvent.DismissError -> {
                    setState { copy(errorMessage = null) }
                }
                is JobsEvent.HandleError -> {
                    handleErrorAction(event.action)
                }
                is JobsEvent.SearchQueryChanged -> {
                    searchJobs(event.query)
                }
                is JobsEvent.ScrollToTop -> {
                    emitEvent(JobsEvent.ScrollToTop)
                }
                is JobsEvent.ShowSnackbar -> {
                    emitEvent(event)
                }
                is JobsEvent.NavigateToJobDetail -> {
                    emitEvent(event)
                }
                is JobsEvent.NetworkStatusChanged -> {
                    setState {
                        copy(
                            isNetworkAvailable = event.isAvailable,
                            lastNetworkCheck = event.timestamp
                        )
                    }
                }
                is JobsEvent.ValidationError -> {
                    setState {
                        copy(
                            validationErrors = event.errors.values.toList()
                        )
                    }
                }
                is JobsEvent.RefreshComplete -> {
                    setState { copy(isRefreshing = false) }
                }
                is JobsEvent.SearchComplete -> {
                    setState {
                        copy(
                            filteredJobsCount = event.resultCount,
                            isSearching = false
                        )
                    }
                }
                is JobsEvent.Refresh -> {
                    refresh()
                }
            }
        }
    }

    private fun loadJobs(forceRefresh: Boolean = false) = safeLaunch {
        setState { copy(isLoading = true) }

        val params = GetJobsUseCase.Params(
            page = state.value.currentPage,
            pageSize = PAGE_SIZE,
            state = filterData.value.state,
            district = filterData.value.district,
            minSalary = filterData.value.minSalary,
            maxSalary = filterData.value.maxSalary,
            forceRefresh = forceRefresh
        )

        try {
            getJobsUseCase(params).collect { result ->
                when (result) {
                    is ApiResult.Success -> handleJobsLoaded(result.data)
                    is ApiResult.Error -> throw convertToPresentationError(result.error)
                    is ApiResult.Loading -> setState { copy(isLoading = true) }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is com.example.gigwork.presentation.base.AppError -> throw e
                else -> throw com.example.gigwork.presentation.base.AppError.UnexpectedError(
                    message = e.message ?: "An unexpected error occurred",
                    cause = e
                )
            }
        }
    }



    private fun handleJobsLoaded(jobs: List<Job>) {
        setState {
            copy(
                jobs = jobs,
                isLoading = false,
                isRefreshing = false,
                totalJobs = jobs.size,
                filteredJobsCount = jobs.size,
                errorMessage = null
            )
        }
    }

    fun applyFilters(filterData: JobsFilterData) = safeLaunch {
        _filterData.value = filterData
        setState { copy(currentPage = INITIAL_PAGE) }
        loadJobs(forceRefresh = true)
    }

    fun refresh() = safeLaunch {
        setState { copy(isRefreshing = true) }
        loadJobs(forceRefresh = true)
        emitEvent(JobsEvent.RefreshComplete)
    }

    fun loadNextPage() = safeLaunch {
        if (state.value.isLastPage || state.value.isLoadingNextPage) return@safeLaunch

        setState { copy(isLoadingNextPage = true) }

        val nextPage = state.value.currentPage + 1
        setState { copy(currentPage = nextPage) }
        loadJobs()
    }

    fun searchJobs(query: String) = safeLaunch {
        if (query == filterData.value.searchQuery) return@safeLaunch

        setState {
            copy(
                isSearching = true,
                searchQuery = query
            )
        }

        jobRepository.searchJobs(query).collect { result ->
            when (result) {
                is ApiResult.Success -> {
                    setState {
                        copy(
                            jobs = result.data,
                            isSearching = false,
                            searchError = null,
                            filteredJobsCount = result.data.size
                        )
                    }
                    emitEvent(JobsEvent.SearchComplete(result.data.size))
                }
                is ApiResult.Error -> {
                    setState {
                        copy(
                            isSearching = false,
                            searchError = result.error.message
                        )
                    }
                }
                is ApiResult.Loading -> setState { copy(isSearching = true) }
            }
        }
    }



    fun toggleBookmark(jobId: String) = safeLaunch {
        try {
            val isBookmarked = bookmarkRepository.isJobBookmarked(jobId).first()
            when (isBookmarked) {
                is ApiResult.Success -> {
                    if (isBookmarked.data) {
                        bookmarkRepository.removeBookmark(jobId)
                    } else {
                        bookmarkRepository.addBookmark(jobId)
                    }
                    emitEvent(JobsEvent.ShowSnackbar(
                        if (isBookmarked.data) "Bookmark removed" else "Job bookmarked"
                    ))
                }
                is ApiResult.Error -> {
                    val presentationError = convertToPresentationError(isBookmarked.error)
                    handleError(presentationError)
                }
                is ApiResult.Loading -> {} // Handle loading if needed
            }
        } catch (e: Exception) {
            val presentationError = convertToPresentationError(e)
            handleError(presentationError)
        }
    }

    fun navigateToJobDetail(jobId: String) = safeLaunch {
        emitEvent(JobsEvent.NavigateToJobDetail(jobId))
    }

    private fun observeNetworkChanges() {
        viewModelScope.launch {
            _networkAvailable.collect { isAvailable ->
                setState { copy(isNetworkAvailable = isAvailable) }
                emitEvent(JobsEvent.NetworkStatusChanged(isAvailable))

                if (isAvailable && state.value.hasNetworkError) {
                    retry()
                }
            }
        }
    }

    fun retry() {
        loadJobs(forceRefresh = true)
    }

    fun clearSearchQuery() {
        setState { copy(searchQuery = "") }
        loadJobs(forceRefresh = true)
    }

    fun scrollToTop() {
        viewModelScope.launch {
            emitEvent(JobsEvent.ScrollToTop)
        }
    }

    // Job Metrics Calculations
    private val _jobStatistics = MutableStateFlow<JobStatisticsRepository.JobStatistics?>(null)
    private val jobStatistics: StateFlow<JobStatisticsRepository.JobStatistics?> = _jobStatistics.asStateFlow()

    private val _employerRating = MutableStateFlow<EmployerRatingRepository.EmployerRating?>(null)
    private val employerRating: StateFlow<EmployerRatingRepository.EmployerRating?> = _employerRating.asStateFlow()

    private fun calculateJobMetrics(job: Job) {
        val metrics = JobMetrics(
            totalCost = calculateTotalCost(job),
            durationInDays = calculateDurationInDays(job),
            costPerDay = calculateCostPerDay(job),
            isLongTerm = job.workDuration > 30,
            applicationsCount = jobStatistics.value?.applicationsCount ?: 0,
            viewsCount = jobStatistics.value?.viewsCount ?: 0,
            averageApplicantRating = jobStatistics.value?.averageApplicantRating ?: 0.0,
            activeApplicationsCount = jobStatistics.value?.activeApplicationsCount ?: 0,
            employerResponseRate = employerRating.value?.responseRate ?: 0.0,
            employerRating = employerRating.value?.rating ?: 0.0
        )

        setState { copy(jobMetrics = metrics) }
    }

    private suspend fun loadJobStatistics(jobId: String) {
        jobStatisticsRepository.getJobStatistics(jobId)
            .collect { result ->
                when (result) {
                    is ApiResult.Success -> _jobStatistics.value = result.data
                    is ApiResult.Error -> throw convertToPresentationError(result.error)
                    is ApiResult.Loading -> {} // Loading state handled elsewhere
                }
            }
    }

    private suspend fun loadEmployerRating(employerId: String) {
        employerRatingRepository.getEmployerRating(employerId)
            .collect { result ->
                when (result) {
                    is ApiResult.Success -> _employerRating.value = result.data
                    is ApiResult.Error -> throw convertToPresentationError(result.error)
                    is ApiResult.Loading -> {} // Loading state handled elsewhere
                }
            }
    }
    private fun calculateTotalCost(job: Job): Double {
        return when (job.salaryUnit.lowercase()) {
            "hourly" -> calculateHourlyTotal(job)
            "daily" -> calculateDailyTotal(job)
            "weekly" -> calculateWeeklyTotal(job)
            "monthly" -> calculateMonthlyTotal(job)
            else -> 0.0
        }
    }

    private fun calculateHourlyTotal(job: Job): Double {
        return when (job.workDurationUnit.lowercase()) {
            "hours" -> job.salary * job.workDuration
            "days" -> job.salary * job.workDuration * 8 // 8 hours per day
            "weeks" -> job.salary * job.workDuration * 40 // 40 hours per week
            "months" -> job.salary * job.workDuration * 160 // 160 hours per month
            else -> 0.0
        }
    }

    private fun calculateDailyTotal(job: Job): Double {
        return when (job.workDurationUnit.lowercase()) {
            "hours" -> job.salary * (job.workDuration / 8.0)
            "days" -> job.salary * job.workDuration
            "weeks" -> job.salary * job.workDuration * 5 // 5 days per week
            "months" -> job.salary * job.workDuration * 22 // 22 days per month
            else -> 0.0
        }
    }

    private fun calculateWeeklyTotal(job: Job): Double {
        return when (job.workDurationUnit.lowercase()) {
            "hours" -> job.salary * (job.workDuration / 40.0)
            "days" -> job.salary * (job.workDuration / 5.0)
            "weeks" -> job.salary * job.workDuration
            "months" -> job.salary * job.workDuration * 4.33 // 4.33 weeks per month
            else -> 0.0
        }
    }

    private fun calculateMonthlyTotal(job: Job): Double {
        return when (job.workDurationUnit.lowercase()) {
            "hours" -> job.salary * (job.workDuration / 160.0)
            "days" -> job.salary * (job.workDuration / 22.0)
            "weeks" -> job.salary * (job.workDuration / 4.33)
            "months" -> job.salary * job.workDuration
            else -> 0.0
        }
    }

    fun calculateDurationInDays(job: Job): Int {
        return when (job.workDurationUnit.lowercase()) {
            "hours" -> (job.workDuration / 8)
            "days" -> job.workDuration
            "weeks" -> job.workDuration * 7
            "months" -> job.workDuration * 30
            else -> 0
        }
    }

    fun calculateCostPerDay(job: Job): Double {
        val totalCost = calculateTotalCost(job)
        val durationInDays = calculateDurationInDays(job)
        return if (durationInDays > 0) totalCost / durationInDays else 0.0
    }

    // Application Management
    fun applyForJob(attachments: List<Uri>) = safeLaunch {
        if (!validateApplication(attachments)) return@safeLaunch

        setState { copy(isApplying = true) }
        try {
            val jobId = state.value.job?.id ?: return@safeLaunch
            val files = uploadAttachments(attachments)

            applicationRepository.submitApplication(
                JobApplicationRepository.JobApplication(
                    jobId = jobId,
                    attachments = files,
                    appliedAt = System.currentTimeMillis()
                )
            ).collect { result ->
                when (result) {
                    is ApiResult.Success -> handleApplicationSuccess(jobId)
                    is ApiResult.Error -> throw convertToPresentationError(result.error)
                    is ApiResult.Loading -> setState { copy(isApplying = true) }
                }
            }
        } catch (e: Exception) {
            throw convertToPresentationError(e)
        } finally {
            setState { copy(isApplying = false) }
        }
    }

    private suspend fun uploadAttachments(attachments: List<Uri>): List<String> {
        return attachments.mapNotNull { uri ->
            try {
                val result = fileRepository.uploadFile(uri).first()
                when (result) {
                    is ApiResult.Success -> result.data
                    else -> null
                }
            } catch (e: Exception) {
                throw convertToPresentationError(e)
                null
            }
        }
    }

    private fun validateApplication(attachments: List<Uri>): Boolean {
        val errors = mutableListOf<String>()

        // Check application deadline
        state.value.job?.let { job ->
            if (isApplicationDeadlinePassed(job)) {
                errors.add("Application deadline has passed")
            }
        }

        // Validate attachments
        attachments.forEach { uri ->
            val fileSize = fileRepository.getFileSize(uri)
            if (fileSize > MAX_FILE_SIZE) {
                errors.add("File size exceeds maximum limit of 5MB")
            }
        }

        if (errors.isNotEmpty()) {
            setState { copy(validationErrors = errors) }
            return false
        }
        return true
    }

    private fun isApplicationDeadlinePassed(job: Job): Boolean {
        val deadline = job.applicationDeadline ?: return false
        return System.currentTimeMillis() > deadline
    }

    // Status Management
    fun updateJobStatus(newStatus: String) = safeLaunch {
        if (!validateStatusUpdate(newStatus)) return@safeLaunch

        setState { copy(isUpdating = true) }
        try {
            jobRepository.updateJobStatus(jobId = state.value.job?.id ?: return@safeLaunch, newStatus)
                .collect { result ->
                    when (result) {
                        is ApiResult.Success -> handleStatusUpdateSuccess(result.data)
                        is ApiResult.Error -> throw convertToPresentationError(result.error)
                        is ApiResult.Loading -> setState { copy(isUpdating = true) }
                    }
                }
        } catch (e: Exception) {
            throw convertToPresentationError(e)
        } finally {
            setState { copy(isUpdating = false) }
        }
    }

    private fun validateStatusUpdate(newStatus: String): Boolean {
        val currentJob = state.value.job ?: return false
        return when {
            !canUpdateStatus(currentJob) -> {
                setState {
                    copy(
                        errorMessage = com.example.gigwork.core.error.model.ErrorMessage(
                            message = "You don't have permission to update this job",
                            level = com.example.gigwork.core.error.model.ErrorLevel.ERROR
                        )
                    )
                }
                false
            }
            !isValidStatusTransition(currentJob.status, newStatus) -> {
                setState {
                    copy(
                        errorMessage = com.example.gigwork.core.error.model.ErrorMessage(
                            message = "Invalid status transition",
                            level = com.example.gigwork.core.error.model.ErrorLevel.ERROR
                        )
                    )
                }
                false
            }
            else -> true
        }
    }
    private fun getCurrentUserId(): String {
        return currentUserId ?: throw IllegalStateException("User ID not initialized")
    }

    private fun canUpdateStatus(job: Job): Boolean {
        return job.employerId == getCurrentUserId()
    }

    private fun isValidStatusTransition(currentStatus: String, newStatus: String): Boolean {
        return when (currentStatus.uppercase()) {
            "OPEN" -> newStatus.uppercase() in listOf("CLOSED", "PAUSED", "DELETED")
            "PAUSED" -> newStatus.uppercase() in listOf("OPEN", "CLOSED", "DELETED")
            "CLOSED" -> newStatus.uppercase() in listOf("DELETED")
            else -> false
        }
    }

    private fun handleStatusUpdateSuccess(job: Job) {
        setState {
            copy(
                job = job,
                isUpdating = false,
                successMessage = "Job status updated successfully"
            )
        }
    }

    private fun handleApplicationSuccess(jobId: String) {
        val currentState = state.value
        val updatedAppliedJobIds = currentState.appliedJobIds + jobId

        setState {
            copy(
                appliedJobIds = updatedAppliedJobIds,
                isApplying = false,
                successMessage = "Application submitted successfully"
            )
        }

        safeLaunch {
            emitEvent(JobsEvent.ShowSnackbar("Application submitted successfully"))
            updateApplicationStatistics(jobId)
        }
    }

    private suspend fun updateApplicationStatistics(jobId: String) {
        try {
            jobStatisticsRepository.incrementApplicationCount(jobId)
                .collect { result ->
                    when (result) {
                        is ApiResult.Success -> loadJobs(forceRefresh = true)
                        is ApiResult.Error -> logDebug(TAG, "Failed to update job statistics: ${result.error.message}")
                        is ApiResult.Loading -> {} // Loading state handled by loadJobs
                    }
                }
        } catch (error: Exception) {
            logDebug(TAG, "Failed to update job statistics: ${error.message ?: "Unknown error"}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            state.value.job?.let { job ->
                try {
                    jobStatisticsRepository.incrementViewCount(job.id).collect { result ->
                        when (result) {
                            is ApiResult.Success -> {
                                logDebug(TAG, "View count incremented successfully for job ${job.id}")
                            }
                            is ApiResult.Error -> {
                                logError(
                                    tag = TAG,
                                    message = "Failed to increment view count",
                                    throwable = result.error
                                )
                            }
                            is ApiResult.Loading -> {
                                // Loading state can be ignored in cleanup
                            }
                        }
                    }
                } catch (e: Exception) {
                    logError(
                        tag = TAG,
                        message = "Failed to increment view count",
                        throwable = e
                    )
                }
            }
        }
    }


}