package com.example.gigwork.domain.repository

import com.example.gigwork.core.result.ApiResult
import kotlinx.coroutines.flow.Flow

interface JobApplicationRepository {
    /**
     * Submit a new job application
     * @param application JobApplication containing application details
     * @return Flow of ApiResult containing the application ID
     */
    suspend fun submitApplication(application: JobApplication): Flow<ApiResult<String>>

    /**
     * Get application status for a specific job
     * @param jobId Unique identifier of the job
     * @return Flow of ApiResult containing the application status
     */
    suspend fun getApplicationStatus(jobId: String): Flow<ApiResult<ApplicationStatus>>

    /**
     * Withdraw an existing application
     * @param jobId Unique identifier of the job
     * @return Flow of ApiResult indicating success or failure
     */
    suspend fun withdrawApplication(jobId: String): Flow<ApiResult<Boolean>>

    /**
     * Get all applications for a specific job
     * @param jobId Unique identifier of the job
     * @return Flow of ApiResult containing list of applications
     */
    suspend fun getJobApplications(jobId: String): Flow<ApiResult<List<JobApplication>>>

    /**
     * Get user's applications
     * @return Flow of ApiResult containing list of user's applications
     */
    suspend fun getUserApplications(): Flow<ApiResult<List<JobApplication>>>

    data class JobApplication(
        val jobId: String,
        val userId: String? = null,
        val attachments: List<String> = emptyList(),
        val appliedAt: Long,
        val status: ApplicationStatus = ApplicationStatus.PENDING
    )

    enum class ApplicationStatus {
        NOT_APPLIED,
        PENDING,
        ACCEPTED,
        REJECTED,
        WITHDRAWN
    }
}