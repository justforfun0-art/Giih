package com.example.gigwork.presentation.states

import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.domain.models.Job
import com.example.gigwork.presentation.base.UiState

data class JobDetailsState(
    val isLoading: Boolean = false,
    val errorMessage: ErrorMessage? = null,
    // Job Data
    val job: Job? = null,
    val jobMetrics: JobMetrics? = null,
    val similarJobs: List<Job> = emptyList(),

    // Application States
    val isApplied: Boolean = false,
    val canApply: Boolean = false,
    val isApplying: Boolean = false,
    val applicationStatus: String? = null,

    // Operation States
    val isUpdating: Boolean = false,
    val isBookmarking: Boolean = false,
    val isReporting: Boolean = false,
    val canUpdate: Boolean = false,

    // User Interaction
    val successMessage: String? = null,
    val validationErrors: List<String> = emptyList(),

    // Location & Metrics
    val distanceFromUser: Double? = null,
    val employerRating: Double? = null,
) : UiState

data class JobMetrics(
    val totalCost: Double = 0.0,
    val durationInDays: Int = 0,
    val costPerDay: Double = 0.0,
    val isLongTerm: Boolean = false,
    val applicationsCount: Int = 0,
    val viewsCount: Int = 0,
    val averageApplicantRating: Double = 0.0,
    val activeApplicationsCount: Int = 0,
    val employerResponseRate: Double = 0.0,
    val employerRating: Double = 0.0
)

sealed class JobDetailsEvent {
    data class ApplicationSubmitted(val jobId: String) : JobDetailsEvent()
    data class StatusUpdated(val newStatus: String) : JobDetailsEvent()
    data class NavigateToChat(
        val employerId: String,
        val jobId: String,
        val jobTitle: String
    ) : JobDetailsEvent()

    data class NavigateToLocation(
        val latitude: Double,
        val longitude: Double,
        val title: String,
        val address: String
    ) : JobDetailsEvent()

    data class ShareJob(val shareData: ShareData) : JobDetailsEvent()
    data class JobReported(val reportId: String) : JobDetailsEvent()
    data class NavigateToEmployerProfile(val employerId: String) : JobDetailsEvent()
    object BookmarkUpdated : JobDetailsEvent()
}

data class ShareData(
    val title: String,
    val description: String,
    val salary: String,
    val location: String,
    val applicationLink: String,
    val additionalInfo: Map<String, String>
)