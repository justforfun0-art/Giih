package com.example.gigwork.data.repository

import android.R.attr.order
import com.example.gigwork.core.error.ExceptionMapper
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.data.database.dao.JobDao
import com.example.gigwork.data.database.toDomain
import com.example.gigwork.data.database.toEntity
import com.example.gigwork.data.models.JobDto
import com.example.gigwork.data.mappers.toDomain
import com.example.gigwork.data.mappers.toDto
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.repository.JobRepository
import com.example.gigwork.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.sql.SQLException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import com.example.gigwork.domain.models.DashboardMetrics



@Singleton
class JobRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val jobDao: JobDao,
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : JobRepository {

    companion object {
        private const val TAG = "JobRepository"
        private const val CACHE_DURATION = 30 * 60 * 1000L // 30 minutes
        private const val PAGE_SIZE = 20
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
    ): Flow<ApiResult<List<Job>>> = flow {
        emit(ApiResult.Loading)
        try {
            val offset = (page - 1) * pageSize

            // First, try to get from cache
            jobDao.getPagedJobs(
                state = state,
                district = district,
                minSalary = minSalary,
                maxSalary = maxSalary,
                pageSize = pageSize,
                offset = offset
            ).collect { cachedJobs ->
                if (cachedJobs.isNotEmpty()) {
                    logger.d(
                        tag = TAG,
                        message = "Returning cached jobs",
                        additionalData = mapOf(
                            "cached_count" to cachedJobs.size,
                            "page" to page,
                            "state" to state,
                            "district" to district
                        )
                    )
                    emit(ApiResult.Success(cachedJobs.map { it.toDomain() }))
                }
            }

            // Then fetch from network
            withContext(ioDispatcher) {
                try {
                    // First get all jobs matching non-spatial filters
                    val filteredJobs = supabaseClient.client.postgrest["jobs"]
                        .select {
                            filter {
                                state?.let { eq("location_state", it) }
                                district?.let { eq("location_district", it) }
                                minSalary?.let { gte("salary", it) }
                                maxSalary?.let { lte("salary", it) }
                            }

                            // Pagination
                            val rangeFrom = offset.toLong()
                            val rangeTo = (offset + pageSize - 1).toLong()
                            range(rangeFrom..rangeTo)

                            // Ordering
                            order(column = "created_at", order = Order.DESCENDING)
                        }
                        .decodeList<JobDto>()

                    // Then apply spatial filtering in memory if needed
                    val networkJobs = if (radius != null && latitude != null && longitude != null) {
                        val radiusInMeters = radius * 1000
                        filteredJobs.filter { job ->
                            val jobLat = job.location.latitude
                            val jobLon = job.location.longitude
                            if (jobLat != null && jobLon != null) {
                                calculateDistance(latitude, longitude, jobLat, jobLon) <= radiusInMeters
                            } else {
                                false
                            }
                        }
                    } else {
                        filteredJobs
                    }.map { it.toDomain() }

                    logger.d(
                        tag = TAG,
                        message = "Fetched jobs from network",
                        additionalData = mapOf(
                            "count" to networkJobs.size,
                            "page" to page,
                            "state" to state,
                            "district" to district,
                            "radius" to radius
                        )
                    )

                    // Update cache with new data
                    jobDao.insertJobs(networkJobs.map { it.toEntity() })
                    emit(ApiResult.Success(networkJobs))
                } catch (e: Exception) {
                    logger.e(
                        tag = TAG,
                        message = "Network error fetching jobs",
                        throwable = e,
                        additionalData = mapOf(
                            "page" to page,
                            "state" to state,
                            "district" to district
                        )
                    )

                    // If network fetch fails but we have cached data, don't emit error
                    val jobCount = jobDao.getJobCount()
                    if (jobCount == 0) {
                        emit(ApiResult.Error(ExceptionMapper.map(e, "GET_JOBS_NETWORK")))
                    }
                }
            }
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is IOException -> "Network error fetching jobs"
                is SQLException -> "Database error fetching jobs"
                else -> "Error fetching jobs"
            }

            logger.e(
                tag = TAG,
                message = errorMessage,
                throwable = e,
                additionalData = mapOf(
                    "page" to page,
                    "pageSize" to pageSize,
                    "state" to state,
                    "district" to district,
                    "minSalary" to minSalary,
                    "maxSalary" to maxSalary,
                    "radius" to radius,
                    "latitude" to latitude,
                    "longitude" to longitude
                )
            )

            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_JOBS")))
        }
    }

    // Helper function to calculate the distance between two geographic points using the Haversine formula
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
    override suspend fun getJobById(jobId: String): Flow<ApiResult<Job>> = flow {
        emit(ApiResult.Loading)
        try {
            // Check cache first
            val cachedJob = jobDao.getJobById(jobId)
            if (cachedJob != null) {
                logger.d(
                    tag = TAG,
                    message = "Returning cached job",
                    additionalData = mapOf("job_id" to jobId)
                )
                emit(ApiResult.Success(cachedJob.toDomain()))
            }

            // Fetch from network
            withContext(ioDispatcher) {
                val response = supabaseClient.client.postgrest.from("jobs")
                    .select {
                        filter {
                            eq("id", jobId)
                        }
                            order("created_at", Order.ASCENDING)

                    }

                    .decodeSingle<JobDto>()

                val job = response.toDomain()
                jobDao.insertJob(job.toEntity())

                logger.i(
                    tag = TAG,
                    message = "Successfully fetched job from network",
                    additionalData = mapOf(
                        "job_id" to jobId,
                        "title" to job.title
                    )
                )
                emit(ApiResult.Success(job))
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error fetching job by ID",
                throwable = e,
                additionalData = mapOf("job_id" to jobId)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_JOB_BY_ID")))
        }
    }

    override suspend fun createJob(job: Job): Flow<ApiResult<Job>> = flow {
        emit(ApiResult.Loading)
        try {
            withContext(ioDispatcher) {
                val createdJob = createJobInNetwork(job)
                jobDao.insertJob(createdJob.toEntity())
                logger.i(
                    tag = TAG,
                    message = "Successfully created job",
                    additionalData = mapOf(
                        "job_id" to createdJob.id,
                        "employer_id" to createdJob.employerId,
                        "title" to createdJob.title
                    )
                )
                emit(ApiResult.Success(createdJob))
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error creating job",
                throwable = e,
                additionalData = mapOf(
                    "job_title" to job.title,
                    "employer_id" to job.employerId
                )
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "CREATE_JOB")))
        }
    }

    override suspend fun updateJob(jobId: String, job: Job): Flow<ApiResult<Job>> = flow {
        emit(ApiResult.Loading)
        try {
            withContext(ioDispatcher) {
                val updatedJob = updateJobInNetwork(jobId, job)
                jobDao.insertJob(updatedJob.toEntity())
                logger.i(
                    tag = TAG,
                    message = "Successfully updated job",
                    additionalData = mapOf(
                        "job_id" to jobId,
                        "updated_fields" to getUpdatedFields(job)
                    )
                )
                emit(ApiResult.Success(updatedJob))
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error updating job",
                throwable = e,
                additionalData = mapOf(
                    "job_id" to jobId,
                    "job_title" to job.title
                )
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "UPDATE_JOB")))
        }
    }

    override suspend fun deleteJob(jobId: String): Flow<ApiResult<Boolean>> = flow {
        emit(ApiResult.Loading)
        try {
            withContext(ioDispatcher) {
                deleteJobFromNetwork(jobId)
                jobDao.deleteJob(jobId)
                logger.i(
                    tag = TAG,
                    message = "Successfully deleted job",
                    additionalData = mapOf("job_id" to jobId)
                )
                emit(ApiResult.Success(true))
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error deleting job",
                throwable = e,
                additionalData = mapOf("job_id" to jobId)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "DELETE_JOB")))
        }
    }

    override suspend fun getEmployerJobs(employerId: String): Flow<ApiResult<List<Job>>> = flow {
        emit(ApiResult.Loading)
        try {
            val cacheTimestamp = System.currentTimeMillis()
            jobDao.getEmployerJobs(employerId).collect { cachedJobs ->
                if (cachedJobs.isNotEmpty() && !isCacheExpired(cacheTimestamp)) {
                    logger.d(
                        tag = TAG,
                        message = "Returning cached employer jobs",
                        additionalData = mapOf(
                            "employer_id" to employerId,
                            "job_count" to cachedJobs.size
                        )
                    )
                    emit(ApiResult.Success(cachedJobs.map { it.toDomain() }))
                }
            }

            withContext(ioDispatcher) {
                val networkJobs = fetchEmployerJobsFromNetwork(employerId)
                jobDao.insertJobs(networkJobs.map { it.toEntity() })
                logger.i(
                    tag = TAG,
                    message = "Successfully fetched employer jobs from network",
                    additionalData = mapOf(
                        "employer_id" to employerId,
                        "job_count" to networkJobs.size
                    )
                )
                emit(ApiResult.Success(networkJobs))
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error fetching employer jobs",
                throwable = e,
                additionalData = mapOf("employer_id" to employerId)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_EMPLOYER_JOBS")))
        }
    }


    // Second method (that you need to add)
    override fun getEmployerJobs(
        employerId: String,
        searchQuery: String,
        status: String?,
        minSalary: Double?,
        maxSalary: Double?,
        state: String?,
        district: String?,
        page: Int,
        pageSize: Int
    ): Flow<ApiResult<List<Job>>> = flow {
        emit(ApiResult.Loading)
        try {
            val offset = (page - 1) * pageSize

            // First try to get from cache with filters
            // Note: You'll need to update your JobDao to handle these filters
            jobDao.getFilteredEmployerJobs(
                employerId = employerId,
                query = searchQuery,
                status = status,
                minSalary = minSalary,
                maxSalary = maxSalary,
                state = state,
                district = district,
                limit = pageSize,
                offset = offset
            ).collect { cachedJobs ->
                if (cachedJobs.isNotEmpty()) {
                    logger.d(
                        tag = TAG,
                        message = "Returning filtered cached employer jobs",
                        additionalData = mapOf(
                            "employer_id" to employerId,
                            "job_count" to cachedJobs.size,
                            "page" to page,
                            "filters_applied" to true
                        )
                    )
                    emit(ApiResult.Success(cachedJobs.map { it.toDomain() }))
                }
            }

            // Then fetch from network with filters
            withContext(ioDispatcher) {
                val networkJobs = fetchFilteredEmployerJobsFromNetwork(
                    employerId = employerId,
                    searchQuery = searchQuery,
                    status = status,
                    minSalary = minSalary,
                    maxSalary = maxSalary,
                    state = state,
                    district = district,
                    page = page,
                    pageSize = pageSize
                )

                // Update cache with new data
                jobDao.insertJobs(networkJobs.map { it.toEntity() })

                logger.i(
                    tag = TAG,
                    message = "Successfully fetched filtered employer jobs from network",
                    additionalData = mapOf(
                        "employer_id" to employerId,
                        "job_count" to networkJobs.size,
                        "page" to page,
                        "filters_applied" to true
                    )
                )

                emit(ApiResult.Success(networkJobs))
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error fetching filtered employer jobs",
                throwable = e,
                additionalData = mapOf(
                    "employer_id" to employerId,
                    "search_query" to searchQuery,
                    "status" to status,
                    "page" to page
                )
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_FILTERED_EMPLOYER_JOBS")))
        }
    }

    // Also need to add a method to implement getEmployerDashboardMetrics
    override suspend fun getEmployerDashboardMetrics(employerId: String): DashboardMetrics {
        // Implement the dashboard metrics logic
        // This would typically aggregate data from different sources
        return DashboardMetrics(
            openJobsCount = 0, // Replace with actual implementation
            totalApplicantsCount = 0,
            activeApplicantsCount = 0,
            hiredCount = 0,
            recentJobs = emptyList()
        )
    }

    // Add this helper method to fetch filtered jobs from the network
    private suspend fun fetchFilteredEmployerJobsFromNetwork(
        employerId: String,
        searchQuery: String,
        status: String?,
        minSalary: Double?,
        maxSalary: Double?,
        state: String?,
        district: String?,
        page: Int,
        pageSize: Int
    ): List<Job> {
        val offset = (page - 1) * pageSize

        // Create a query builder and apply all filters in a single block
        val query = supabaseClient.client.postgrest["jobs"]
            .select {
                filter {
                    // Required filter for employer ID
                    eq("employer_id", employerId)

                    // Optional filters
                    status?.let { eq("status", it) }
                    minSalary?.let { gte("salary", it) }
                    maxSalary?.let { lte("salary", it) }
                    state?.let { eq("state", it) }
                    district?.let { eq("location_district", it) }

                    // Search query filter (if provided)
                    if (searchQuery.isNotBlank()) {
                        or {
                            ilike("title", "%$searchQuery%")
                            ilike("description", "%$searchQuery%")
                        }
                    }
                }

                // Pagination
                range(offset.toLong(), (offset + pageSize - 1).toLong())

                // Ordering
                order("created_at", Order.DESCENDING)
            }
            .decodeList<JobDto>()
            .map { it.toDomain() }

        return query
    }


    override suspend fun searchJobs(query: String): Flow<ApiResult<List<Job>>> = flow {
        emit(ApiResult.Loading)
        try {
            jobDao.searchJobs(query).collect { cachedJobs ->
                if (cachedJobs.isNotEmpty()) {
                    logger.d(
                        tag = TAG,
                        message = "Returning cached search results",
                        additionalData = mapOf(
                            "query" to query,
                            "result_count" to cachedJobs.size
                        )
                    )
                    emit(ApiResult.Success(cachedJobs.map { it.toDomain() }))
                }
            }

            withContext(ioDispatcher) {
                val networkJobs = searchJobsInNetwork(query)
                jobDao.insertJobs(networkJobs.map { it.toEntity() })
                logger.i(
                    tag = TAG,
                    message = "Successfully searched jobs from network",
                    additionalData = mapOf(
                        "query" to query,
                        "result_count" to networkJobs.size
                    )
                )
                emit(ApiResult.Success(networkJobs))
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error searching jobs",
                throwable = e,
                additionalData = mapOf("query" to query)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "SEARCH_JOBS")))
        }
    }

    override suspend fun updateJobStatus(jobId: String, status: String): Flow<ApiResult<Job>> =
        flow {
            emit(ApiResult.Loading)
            try {
                withContext(ioDispatcher) {
                    val updatedJob = updateJobStatusInNetwork(jobId, status)
                    jobDao.updateJobWithStatus(jobId, status)
                    logger.i(
                        tag = TAG,
                        message = "Successfully updated job status",
                        additionalData = mapOf(
                            "job_id" to jobId,
                            "new_status" to status
                        )
                    )
                    emit(ApiResult.Success(updatedJob))
                }
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Error updating job status",
                    throwable = e,
                    additionalData = mapOf(
                        "job_id" to jobId,
                        "status" to status
                    )
                )
                emit(ApiResult.Error(ExceptionMapper.map(e, "UPDATE_JOB_STATUS")))
            }
        }

    override suspend fun getNearbyJobs(
        latitude: Double,
        longitude: Double,
        radiusInKm: Double
    ): Flow<ApiResult<List<Job>>> = flow {
        emit(ApiResult.Loading)
        try {
            // Calculate the radius for coordinate-based distance calculation
            val radiusSquared = (radiusInKm / 111.0).pow(2)

            // First attempt to retrieve from cache
            jobDao.getNearbyJobs(latitude, longitude, radiusSquared).collect { cachedJobs ->
                if (cachedJobs.isNotEmpty()) {
                    logger.d(
                        tag = TAG,
                        message = "Returning cached nearby jobs",
                        additionalData = mapOf(
                            "latitude" to latitude,
                            "longitude" to longitude,
                            "radius_km" to radiusInKm,
                            "job_count" to cachedJobs.size
                        )
                    )
                    emit(ApiResult.Success(cachedJobs.map { it.toDomain() }))
                }
            }

            // Then fetch from network and update cache
            withContext(ioDispatcher) {
                val networkJobs = fetchNearbyJobsFromNetwork(latitude, longitude, radiusInKm)

                // Update local cache with new data
                jobDao.insertJobs(networkJobs.map { it.toEntity() })

                logger.i(
                    tag = TAG,
                    message = "Successfully fetched nearby jobs from network",
                    additionalData = mapOf(
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "radius_km" to radiusInKm,
                        "job_count" to networkJobs.size
                    )
                )

                emit(ApiResult.Success(networkJobs))
            }
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is IOException -> "Network error fetching nearby jobs"
                is SQLException -> "Database error fetching nearby jobs"
                else -> "Error fetching nearby jobs"
            }

            logger.e(
                tag = TAG,
                message = errorMessage,
                throwable = e,
                additionalData = mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "radius_km" to radiusInKm
                )
            )

            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_NEARBY_JOBS")))
        }
    }
    // other private functions...

    private suspend fun fetchNearbyJobsFromNetwork(
        latitude: Double,
        longitude: Double,
        radiusInKm: Double
    ): List<Job> {
        // Convert radius to meters for the query
        val distanceInMeters = radiusInKm * 1000

        // Get all jobs from the database
        val allJobs = supabaseClient.client.postgrest.from("jobs")
            .select {
                filter {
                    eq("active", true)
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<JobDto>()

        // Filter jobs by distance using the Haversine formula in memory
        return allJobs
            .filter { job ->
                val jobLat = job.location.latitude
                val jobLon = job.location.longitude
                if (jobLat != null && jobLon != null) {
                    calculateDistance(latitude, longitude, jobLat, jobLon) <= distanceInMeters
                } else {
                    false
                }
            }
            .map { it.toDomain() }
    }

    private suspend fun fetchJobFromNetwork(jobId: String): Job {
        return supabaseClient.client.postgrest["jobs"]
            .select {
                filter {
                eq("id", jobId)
            }}
            .decodeList<JobDto>()
            .first()
            .toDomain()
    }

    private suspend fun createJobInNetwork(job: Job): Job {
        return supabaseClient.client.postgrest["jobs"]
            .insert(job.toDto())
            .decodeList<JobDto>()
            .first()
            .toDomain()
    }

    private suspend fun updateJobInNetwork(jobId: String, job: Job): Job {
        return supabaseClient.client.postgrest["jobs"]
            .update(job.toDto()) {
                filter {
                eq("id", jobId)
            }}
            .decodeList<JobDto>()
            .first()
            .toDomain()
    }

    private suspend fun deleteJobFromNetwork(jobId: String) {
        supabaseClient.client.postgrest.from("jobs")
            .delete {
                filter {
                eq("id", jobId)
            }}
    }

    private suspend fun fetchEmployerJobsFromNetwork(employerId: String): List<Job> {
        return supabaseClient.client.postgrest["jobs"]
            .select {
                filter {
                    eq("employer_id", employerId)
                }
                    order("created_at", Order.DESCENDING)
            }
            .decodeList<JobDto>()
            .map { it.toDomain() }
    }

    private suspend fun searchJobsInNetwork(query: String): List<Job> {
        return supabaseClient.client.postgrest["jobs"]
            .select {
                filter {
                    or {
                        ilike("title", "%$query%")
                        ilike("description", "%$query%")
                    }
                }
                order("created_at", Order.DESCENDING)
            }

            .decodeList<JobDto>()
            .map { it.toDomain() }
    }

    private suspend fun updateJobStatusInNetwork(jobId: String, status: String): Job {
        return supabaseClient.client.postgrest.from("jobs")
            .update(mapOf("status" to status)) {
                filter { eq("id", jobId)
            }}
            .decodeList<JobDto>()
            .first()
            .toDomain()
    }

    // Utility functions

    private fun isCacheExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > CACHE_DURATION
    }

    private fun getUpdatedFields(job: Job): Map<String, Any?> {
        return mapOf(
            "title" to job.title,
            "description" to job.description,
            "salary" to job.salary,
            "salary_unit" to job.salaryUnit,
            "work_duration" to job.workDuration,
            "work_duration_unit" to job.workDurationUnit,
            "location_state" to job.location.state,
            "location_district" to job.location.district,
            "location_latitude" to job.location.latitude,
            "location_longitude" to job.location.longitude,
            "status" to job.status
        ).filterValues { it != null }
    }

    private suspend fun refreshCache(jobs: List<Job>) {
        withContext(ioDispatcher) {
            jobDao.clearAllJobs()
            jobDao.insertJobs(jobs.map { it.toEntity() })
            logger.d(
                tag = TAG,
                message = "Cache refreshed",
                additionalData = mapOf("job_count" to jobs.size)
            )
        }
    }



    private object FilterOperator {
        const val EQ = "eq"
        const val GT = "gt"
        const val GTE = "gte"
        const val LT = "lt"
        const val LTE = "lte"
        const val ILIKE = "ilike"
    }
    }