package com.example.gigwork.data.repository

import android.content.Context
import com.example.gigwork.core.error.ExceptionMapper
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.Result
import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.data.database.JobDao
import com.example.gigwork.data.database.entity.toEntity
import com.example.gigwork.data.database.entity.toDomain
import com.example.gigwork.data.models.JobDto
import com.example.gigwork.data.mappers.toDomain
import com.example.gigwork.data.mappers.toDto
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.repository.JobRepository
import com.example.gigwork.util.NetworkUtils
import com.example.gigwork.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.postgrest.query.FilterOperator
import io.github.jan.supabase.postgrest.query.PostgrestSelectQuery
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.example.gigwork.data.database.JobEntityMapper.toDomainModel
import com.example.gigwork.data.database.JobEntityMapper.toEntityModel

@Singleton
class JobRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val jobDao: JobDao,
    @ApplicationContext private val context: Context,
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : JobRepository {

    companion object {
        private const val TAG = "JobRepository"
        private const val DEFAULT_PAGE_SIZE = 20
    }

    override suspend fun getJobs(
        page: Int,
        pageSize: Int,
        state: String?,
        district: String?,
        radius: Double?,
        latitude: Double?,
        longitude: Double?,
        minSalary: Double?,
        maxSalary: Double?
    ): Flow<Result<List<Job>>> = flow {
        emit(Result.Loading)

        try {
            logger.d(
                tag = TAG,
                message = "Fetching jobs",
                additionalData = mapOf(
                    "page" to page,
                    "pageSize" to pageSize,
                    "hasFilters" to (state != null || district != null || minSalary != null)
                )
            )

            if (!NetworkUtils.isNetworkAvailable(context)) {
                val cachedJobs = jobDao.getPagedJobs(
                    state = state,
                    district = district,
                    minSalary = minSalary,
                    maxSalary = maxSalary,
                    pageSize = pageSize,
                    offset = (page - 1) * pageSize
                ).first()

                if (cachedJobs.isNotEmpty()) {
                    logger.d(TAG, "Returning cached jobs", mapOf("count" to cachedJobs.size))
                    emit(Result.Success(cachedJobs.map { it.toDomainModel() }))
                    return@flow
                }
            }

            val jobs = withContext(ioDispatcher) {
                supabaseClient.client.postgrest["jobs"]
                    .select {
                        applyFilters(
                            state = state,
                            district = district,
                            minSalary = minSalary,
                            maxSalary = maxSalary,
                            latitude = latitude,
                            longitude = longitude,
                            radius = radius
                        )
                        limit(pageSize)
                        offset((page - 1) * pageSize)
                        order("created_at", ascending = false)
                    }
                    .decodeList<JobDto>()
                    .map { it.toDomain() }
            }

            jobDao.insertJobs(jobs.map { it.toEntityModel() })
            emit(Result.Success(jobs))

        } catch (e: Exception) {
            logger.e(TAG, "Error fetching jobs", e)
            emit(Result.Error(ExceptionMapper.map(e, "fetch_jobs")))
        }
    }

    override suspend fun getJobById(jobId: String): Flow<Result<Job>> = flow {
        emit(Result.Loading)

        try {
            logger.d(TAG, "Fetching job details", mapOf("job_id" to jobId))

            val cachedJob = jobDao.getJobById(jobId)?.toDomain()
            if (cachedJob != null) {
                emit(Result.Success(cachedJob))
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    return@flow
                }
            }

            val job = withContext(ioDispatcher) {
                supabaseClient.client.postgrest["jobs"]
                    .select { eq("id", jobId) }
                    .decodeSingle<JobDto>()
                    .toDomain()
            }

            jobDao.insertJob(job.toEntity())
            emit(Result.Success(job))

        } catch (e: Exception) {
            logger.e(TAG, "Error fetching job details", e)
            emit(Result.Error(ExceptionMapper.map(e, "fetch_job_details")))
        }
    }

    override suspend fun createJob(job: Job): Flow<Result<Job>> = flow {
        emit(Result.Loading)

        try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                throw AppError.NetworkError(
                    message = "No internet connection",
                    isConnectionError = true
                )
            }

            logger.d(TAG, "Creating new job", mapOf("title" to job.title))

            val createdJob = withContext(ioDispatcher) {
                supabaseClient.client.postgrest["jobs"]
                    .insert(job.toDto())
                    .decodeSingle<JobDto>()
                    .toDomain()
            }

            jobDao.insertJob(createdJob.toEntity())
            emit(Result.Success(createdJob))

        } catch (e: Exception) {
            logger.e(TAG, "Error creating job", e)
            emit(Result.Error(ExceptionMapper.map(e, "create_job")))
        }
    }

    override suspend fun updateJob(jobId: String, job: Job): Flow<Result<Job>> = flow {
        emit(Result.Loading)

        try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                throw AppError.NetworkError(
                    message = "No internet connection",
                    isConnectionError = true
                )
            }

            val updatedJob = withContext(ioDispatcher) {
                supabaseClient.client.postgrest["jobs"]
                    .update(job.toDto()) { eq("id", jobId) }
                    .decodeSingle<JobDto>()
                    .toDomain()
            }

            jobDao.insertJob(updatedJob.toEntity())
            emit(Result.Success(updatedJob))

        } catch (e: Exception) {
            logger.e(TAG, "Error updating job", e)
            emit(Result.Error(ExceptionMapper.map(e, "update_job")))
        }
    }

    override suspend fun deleteJob(jobId: String): Flow<Result<Boolean>> = flow {
        emit(Result.Loading)

        try {
            withContext(ioDispatcher) {
                supabaseClient.client.postgrest["jobs"]
                    .delete { eq("id", jobId) }
            }

            jobDao.deleteJob(jobId)
            emit(Result.Success(true))

        } catch (e: Exception) {
            logger.e(TAG, "Error deleting job", e)
            emit(Result.Error(ExceptionMapper.map(e, "delete_job")))
        }
    }

    override suspend fun searchJobs(query: String): Flow<Result<List<Job>>> = flow {
        emit(Result.Loading)

        try {
            val cachedJobs = jobDao.searchJobs(query).first()
            if (cachedJobs.isNotEmpty()) {
                emit(Result.Success(cachedJobs.map { it.toDomain() }))
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    return@flow
                }
            }

            val jobs = withContext(ioDispatcher) {
                supabaseClient.client.postgrest["jobs"]
                    .select {
                        or {
                            filter("title", FilterOperator.ILIKE, "%$query%")
                            filter("description", FilterOperator.ILIKE, "%$query%")
                        }
                        limit(DEFAULT_PAGE_SIZE)
                        order("created_at", ascending = false)
                    }
                    .decodeList<JobDto>()
                    .map { it.toDomain() }
            }

            jobDao.insertJobs(jobs.map { it.toEntity() })
            emit(Result.Success(jobs))

        } catch (e: Exception) {
            logger.e(TAG, "Error searching jobs", e)
            emit(Result.Error(ExceptionMapper.map(e, "search_jobs")))
        }
    }

    private suspend fun PostgrestSelectQuery.applyFilters(
        state: String?,
        district: String?,
        minSalary: Double?,
        maxSalary: Double?,
        latitude: Double?,
        longitude: Double?,
        radius: Double?
    ) {
        state?.let {
            filter("location->>'state'", FilterOperator.EQ, it)
        }
        district?.let {
            filter("location->>'district'", FilterOperator.EQ, it)
        }
        minSalary?.let {
            filter("salary", FilterOperator.GTE, it)
        }
        maxSalary?.let {
            filter("salary", FilterOperator.LTE, it)
        }
        if (latitude != null && longitude != null && radius != null) {
            filter(
                "ST_DWithin(location::geography, ST_MakePoint($longitude, $latitude)::geography, ${radius * 1000})"
            )
        }
    }
}