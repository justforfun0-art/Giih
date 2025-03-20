// domain/repository/JobRepository.kt
package com.example.gigwork.domain.repository

import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.DashboardMetrics
import com.example.gigwork.domain.models.Job
import kotlinx.coroutines.flow.Flow

interface JobRepository {
    suspend fun getJobs(
        page: Int = 1,
        pageSize: Int = 20,
        state: String? = null,
        district: String? = null,
        radius: Double? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        minSalary: Double? = null,
        maxSalary: Double? = null
    ): Flow<ApiResult<List<Job>>>

    suspend fun getJobById(jobId: String): Flow<ApiResult<Job>>

    suspend fun getEmployerDashboardMetrics(employerId: String): DashboardMetrics


    suspend fun createJob(job: Job): Flow<ApiResult<Job>>

    suspend fun updateJob(jobId: String, job: Job): Flow<ApiResult<Job>>

    suspend fun deleteJob(jobId: String): Flow<ApiResult<Boolean>>

    suspend fun getEmployerJobs(employerId: String): Flow<ApiResult<List<Job>>>

    suspend fun searchJobs(query: String): Flow<ApiResult<List<Job>>>

    suspend fun updateJobStatus(jobId: String, status: String): Flow<ApiResult<Job>>

    // Add this method to your JobRepository interface
    fun getEmployerJobs(
        employerId: String,
        searchQuery: String = "",
        status: String? = null,
        minSalary: Double? = null,
        maxSalary: Double? = null,
        state: String? = null,
        district: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): Flow<ApiResult<List<Job>>>

    suspend fun getNearbyJobs(
        latitude: Double,
        longitude: Double,
        radiusInKm: Double
    ): Flow<ApiResult<List<Job>>>
}