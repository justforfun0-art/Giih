// domain/repository/JobRepository.kt
package com.example.gigwork.domain.repository

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
    ): Flow<Result<List<Job>>>

    suspend fun getJobById(jobId: String): Flow<Result<Job>>

    suspend fun createJob(job: Job): Flow<Result<Job>>

    suspend fun updateJob(jobId: String, job: Job): Flow<Result<Job>>

    suspend fun deleteJob(jobId: String): Flow<Result<Boolean>>

    suspend fun getEmployerJobs(employerId: String): Flow<Result<List<Job>>>

    suspend fun searchJobs(query: String): Flow<Result<List<Job>>>

    suspend fun updateJobStatus(jobId: String, status: String): Flow<Result<Job>>

    suspend fun getNearbyJobs(
        latitude: Double,
        longitude: Double,
        radiusInKm: Double
    ): Flow<Result<List<Job>>>
}