package com.example.gigwork.presentation.viewmodels

import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.SavedStateHandle
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.gigwork.core.error.extensions.toAppError
import com.example.gigwork.presentation.base.ErrorHandler
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.error.model.ErrorLevel
import com.example.gigwork.core.error.model.ErrorAction
import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.repository.JobRepository
import com.example.gigwork.domain.usecase.job.GetJobsUseCase
import com.example.gigwork.domain.usecase.job.GetUserProfileUseCase
import com.example.gigwork.presentation.base.BaseErrorViewModel
import com.example.gigwork.presentation.states.JobsState
import com.example.gigwork.presentation.states.JobsEvent
import com.example.gigwork.presentation.states.JobsFilterData
import com.example.gigwork.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class JobDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val jobRepository: JobRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val applicationRepository: JobApplicationRepository,
    private val userLocationRepository: UserLocationRepository,
    private val employerRatingRepository: EmployerRatingRepository,
    private val jobStatisticsRepository: JobStatisticsRepository,
    private val fileRepository: FileRepository,
    private val locationManager: LocationManager,
    errorHandler: GlobalErrorHandler,
    logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BaseErrorViewModel<JobDetailsState, JobDetailsEvent>(savedStateHandle, errorHandler, logger) {

    companion object {
        private const val TAG = "JobDetailsViewModel"
        private const val KEY_JOB_ID = "jobId"
        private const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB
        private const val APPLICATION_DEADLINE_HOURS = 24
        private const val SIMILAR_JOBS_LIMIT = 5
    }

    private val jobId: String = checkNotNull(savedStateHandle[KEY_JOB_ID])
    private val _similarJobs = MutableStateFlow<List<Job>>(emptyList())
    private val _employerRating = MutableStateFlow<EmployerRating?>(null)
    private val _jobStatistics = MutableStateFlow<JobStatistics?>(null)
    private val _applicationStatus = MutableStateFlow<ApplicationStatus?>(null)
    private val _bookmarkStatus = MutableStateFlow(false)

    override fun createInitialState() = JobDetailsState()

    init {
        initializeData()
    }

    private fun initializeData() = safeLaunch {
        setState { copy(isLoading = true) }
        loadJobDetails()
        loadSimilarJobs()
        loadEmployerRating()
        loadJobStatistics()
        checkApplicationStatus()
        checkBookmarkStatus()
        calculateDistanceFromUser()
    }

    // Job Details Loading
    private suspend fun loadJobDetails() {
        try {
            jobRepository.getJobById(jobId).collect { result ->
                when (result) {
                    is Result.Success -> handleJobLoaded(result.data)
                    is Result.Error -> handleError(result.error)
                    is Result.Loading -> setState { copy(isLoading = true) }
                }
            }
        } catch (e: Exception) {
            handleError(e.toAppError())
        }
    }

    // Similar Jobs
    private suspend fun loadSimilarJobs() {
        state.value.job?.let { job ->
            jobRepository.getSimilarJobs(
                jobId = job.id,
                category = job.category,
                location = job.location,
                limit = SIMILAR_JOBS_LIMIT
            ).collect { result ->
                when (result) {
                    is Result.Success -> _similarJobs.value = result.data
                    is Result.Error -> handleError(result.error)
                    is Result.Loading -> {} // Already showing loading state
                }
            }
        }
    }

    // Employer Rating
    private suspend fun loadEmployerRating() {
        state.value.job?.let { job ->
            employerRatingRepository.getEmployerRating(job.employerId)
                .collect { result ->
                    when (result) {
                        is Result.Success -> _employerRating.value = result.data
                        is Result.Error -> handleError(result.error)
                        is Result.Loading -> {} // Already showing loading state
                    }
                }
        }
    }

    // Job Statistics
    private suspend fun loadJobStatistics() {
        jobStatisticsRepository.getJobStatistics(jobId)
            .collect { result ->
                when (result) {
                    is Result.Success -> _jobStatistics.value = result.data
                    is Result.Error -> handleError(result.error)
                    is Result.Loading -> {} // Already showing loading state
                }
            }
    }

    // Application Status
    private suspend fun checkApplicationStatus() {
        applicationRepository.getApplicationStatus(jobId)
            .collect { result ->
                when (result) {
                    is Result.Success -> _applicationStatus.value = result.data
                    is Result.Error -> handleError(result.error)
                    is Result.Loading -> {} // Already showing loading state
                }
            }
    }

    // Bookmark Functions
    private suspend fun checkBookmarkStatus() {
        bookmarkRepository.isJobBookmarked(jobId)
            .collect { result ->
                when (result) {
                    is Result.Success -> _bookmarkStatus.value = result.data
                    is Result.Error -> handleError(result.error)
                    is Result.Loading -> {} // Already showing loading state
                }
            }
    }

    fun toggleBookmark() = safeLaunch {
        setState { copy(isBookmarking = true) }
        try {
            if (_bookmarkStatus.value) {
                bookmarkRepository.removeBookmark(jobId)
            } else {
                bookmarkRepository.addBookmark(jobId)
            }
            _bookmarkStatus.value = !_bookmarkStatus.value
            setState {
                copy(
                    isBookmarking = false,
                    successMessage = if (_bookmarkStatus.value) "Job bookmarked" else "Bookmark removed"
                )
            }
        } catch (e: Exception) {
            handleError(e.toAppError())
        } finally {
            setState { copy(isBookmarking = false) }
        }
    }

    // Job Application
    fun applyForJob(attachments: List<Uri>) = safeLaunch {
        if (!validateApplication(attachments)) return@safeLaunch

        setState { copy(isApplying = true) }
        try {
            val files = uploadAttachments(attachments)
            applicationRepository.submitApplication(
                JobApplication(
                    jobId = jobId,
                    attachments = files,
                    appliedAt = System.currentTimeMillis()
                )
            ).collect { result ->
                when (result) {
                    is Result.Success -> handleApplicationSuccess()
                    is Result.Error -> handleError(result.error)
                    is Result.Loading -> setState { copy(isApplying = true) }
                }
            }
        } catch (e: Exception) {
            handleError(e.toAppError())
        } finally {
            setState { copy(isApplying = false) }
        }
    }

    private suspend fun uploadAttachments(attachments: List<Uri>): List<String> {
        return attachments.mapNotNull { uri ->
            try {
                fileRepository.uploadFile(uri).first()
            } catch (e: Exception) {
                handleError(e.toAppError())
                null
            }
        }
    }

    // Job Reporting
    fun reportJob(reason: String, details: String) = safeLaunch {
        setState { copy(isReporting = true) }
        try {
            jobRepository.reportJob(
                JobReport(
                    jobId = jobId,
                    reason = reason,
                    details = details,
                    reportedAt = System.currentTimeMillis()
                )
            ).collect { result ->
                when (result) {
                    is Result.Success -> handleReportSuccess()
                    is Result.Error -> handleError(result.error)
                    is Result.Loading -> setState { copy(isReporting = true) }
                }
            }
        } catch (e: Exception) {
            handleError(e.toAppError())
        } finally {
            setState { copy(isReporting = false) }
        }
    }

    // Location & Distance
    private suspend fun calculateDistanceFromUser() {
        val userLocation = userLocationRepository.getUserLocation()
        state.value.job?.location?.let { jobLocation ->
            val distance = locationManager.calculateDistance(
                userLocation.latitude,
                userLocation.longitude,
                jobLocation.latitude ?: return@let,
                jobLocation.longitude ?: return@let
            )
            setState { copy(distanceFromUser = distance) }
        }
    }

    // Validation Functions
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

    // Success Handlers
    private fun handleJobLoaded(job: Job) {
        setState {
            copy(
                isLoading = false,
                job = job,
                isApplied = job.isApplied,
                canApply = canApplyForJob(job),
                canUpdate = job.employerId == getCurrentUserId(),
                errorMessage = null
            )
        }
        calculateJobMetrics(job)
    }

    private fun handleApplicationSuccess() {
        setState {
            copy(
                isApplied = true,
                canApply = false,
                successMessage = "Application submitted successfully"
            )
        }
        safeLaunch {
            emitEvent(JobDetailsEvent.ApplicationSubmitted)
            updateJobStatistics()
        }
    }

    private fun handleReportSuccess() {
        setState {
            copy(
                isReporting = false,
                successMessage = "Job reported successfully"
            )
        }
        safeLaunch {
            emitEvent(JobDetailsEvent.JobReported)
        }
    }

// Continuing from previous implementation...

    // Job Metrics Calculations
    private fun calculateJobMetrics(job: Job) {
        val metrics = JobMetrics(
            totalCost = calculateTotalCost(job),
            durationInDays = calculateDurationInDays(job),
            costPerDay = calculateCostPerDay(job),
            isLongTerm = job.workDuration > 30,
            applicationsCount = _jobStatistics.value?.applicationsCount ?: 0,
            viewsCount = _jobStatistics.value?.viewsCount ?: 0,
            averageApplicantRating = _jobStatistics.value?.averageApplicantRating ?: 0.0,
            activeApplicationsCount = _jobStatistics.value?.activeApplicationsCount ?: 0,
            employerResponseRate = _employerRating.value?.responseRate ?: 0.0,
            employerRating = _employerRating.value?.rating ?: 0.0
        )
        setState { copy(jobMetrics = metrics) }
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

    // Status Management
    fun updateJobStatus(newStatus: String) = safeLaunch {
        if (!validateStatusUpdate(newStatus)) return@safeLaunch

        setState { copy(isUpdating = true) }
        try {
            jobRepository.updateJobStatus(jobId, newStatus).collect { result ->
                when (result) {
                    is Result.Success -> handleStatusUpdate(result.data)
                    is Result.Error -> handleError(result.error)
                    is Result.Loading -> setState { copy(isUpdating = true) }
                }
            }
        } catch (e: Exception) {
            handleError(e.toAppError())
        } finally {
            setState { copy(isUpdating = false) }
        }
    }

    private fun validateStatusUpdate(newStatus: String): Boolean {
        val currentJob = state.value.job ?: return false
        return when {
            !canUpdateStatus(currentJob) -> {
                setState { copy(errorMessage = "You don't have permission to update this job") }
                false
            }
            !isValidStatusTransition(currentJob.status, newStatus) -> {
                setState { copy(errorMessage = "Invalid status transition") }
                false
            }
            else -> true
        }
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

    // Statistics Tracking
    private suspend fun updateJobStatistics() {
        try {
            jobStatisticsRepository.incrementApplicationCount(jobId)
            loadJobStatistics() // Reload statistics
        } catch (e: Exception) {
            logger.e(TAG, "Failed to update job statistics", e)
        }
    }

    fun incrementViewCount() = safeLaunch {
        try {
            jobStatisticsRepository.incrementViewCount(jobId)
            loadJobStatistics() // Reload statistics
        } catch (e: Exception) {
            logger.e(TAG, "Failed to increment view count", e)
        }
    }

    // Sharing Functionality
    fun shareJob() = safeLaunch {
        state.value.job?.let { job ->
            val shareData = createShareData(job)
            emitEvent(JobDetailsEvent.ShareJob(shareData))
        }
    }

    private fun createShareData(job: Job): ShareData {
        return ShareData(
            title = "Job Opportunity: ${job.title}",
            description = job.description,
            salary = formatSalary(job),
            location = formatLocation(job.location),
            applicationLink = generateApplicationLink(job.id),
            additionalInfo = mapOf(
                "Duration" to formatDuration(job),
                "Posted" to formatDate(job.createdAt),
                "Company" to job.employerName
            )
        )
    }

    // Chat/Communication
    fun startChat() = safeLaunch {
        state.value.job?.let { job ->
            emitEvent(JobDetailsEvent.NavigateToChat(
                employerId = job.employerId,
                jobId = job.id,
                jobTitle = job.title
            ))
        }
    }

    // Location Navigation
    fun navigateToLocation() = safeLaunch {
        state.value.job?.let { job ->
            job.location.let { location ->
                if (location.latitude != null && location.longitude != null) {
                    emitEvent(JobDetailsEvent.NavigateToLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        title = job.title,
                        address = formatLocation(location)
                    ))
                }
            }
        }
    }

    // Utility Functions
    private fun canApplyForJob(job: Job): Boolean {
        return job.status == "OPEN" &&
                !job.isApplied &&
                !isApplicationDeadlinePassed(job) &&
                job.employerId != getCurrentUserId()
    }

    private fun formatSalary(job: Job): String {
        val amount = NumberFormat.getCurrencyInstance().format(job.salary)
        return "$amount per ${job.salaryUnit}"
    }

    private fun formatDuration(job: Job): String {
        return "${job.workDuration} ${job.workDurationUnit}"
    }

    private fun formatLocation(location: Location): String {
        return "${location.district}, ${location.state}"
    }

    private fun generateApplicationLink(jobId: String): String {
        return "https://yourapp.com/jobs/$jobId/apply"
    }

    private fun getCurrentUserId(): String {
        return getSavedState("userId", "")
    }

    // Error Handling
    override fun handleError(error: AppError) {
        logger.e(
            tag = TAG,
            message = "Error in JobDetailsViewModel",
            throwable = error,
            additionalData = mapOf(
                "jobId" to jobId,
                "operation" to getOperationName(),
                "timestamp" to System.currentTimeMillis()
            )
        )

        setState {
            copy(
                isLoading = false,
                isApplying = false,
                isUpdating = false,
                isBookmarking = false,
                isReporting = false,
                errorMessage = error.toErrorMessage()
            )
        }
    }

    private fun getOperationName(): String {
        return when {
            state.value.isApplying -> "applying"
            state.value.isUpdating -> "updating"
            state.value.isBookmarking -> "bookmarking"
            state.value.isReporting -> "reporting"
            state.value.isLoading -> "loading"
            else -> "unknown"
        }
    }

    // State Cleanup
    override fun onCleared() {
        viewModelScope.launch {
            incrementViewCount()
        }
        super.onCleared()
    }

    fun retry() {
        initializeData()
    }

    fun clearError() {
        setState { copy(errorMessage = null) }
    }

    fun clearSuccess() {
        setState { copy(successMessage = null) }
    }
}