package com.example.gigwork.domain.repository

import com.example.gigwork.core.result.ApiResult
import kotlinx.coroutines.flow.Flow

interface JobStatisticsRepository {
    /**
     * Get statistics for a specific job
     * @param jobId Unique identifier of the job
     * @return Flow of ApiResult containing job statistics
     */
    suspend fun getJobStatistics(jobId: String): Flow<ApiResult<JobStatistics>>

    /**
     * Increment view count for a job
     * @param jobId Unique identifier of the job
     * @return Flow of ApiResult indicating success or failure
     */
    suspend fun incrementViewCount(jobId: String): Flow<ApiResult<Boolean>>

    /**
     * Increment application count for a job
     * @param jobId Unique identifier of the job
     * @return Flow of ApiResult indicating success or failure
     */
    suspend fun incrementApplicationCount(jobId: String): Flow<ApiResult<Boolean>>

    /**
     * Get view statistics for multiple jobs
     * @param jobIds List of job IDs
     * @return Flow of ApiResult containing map of job IDs to view counts
     */
    suspend fun getJobsViewStatistics(jobIds: List<String>): Flow<ApiResult<Map<String, Int>>>

    /**
     * Get application statistics for multiple jobs
     * @param jobIds List of job IDs
     * @return Flow of ApiResult containing map of job IDs to application counts
     */
    suspend fun getJobsApplicationStatistics(jobIds: List<String>): Flow<ApiResult<Map<String, Int>>>

    data class JobStatistics(
        val jobId: String,
        val viewsCount: Int = 0,
        val applicationsCount: Int = 0,
        val activeApplicationsCount: Int = 0,
        val averageApplicantRating: Double = 0.0,
        val lastUpdated: Long = System.currentTimeMillis()
    )
}