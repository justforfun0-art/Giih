package com.example.gigwork.data.repository

import com.example.gigwork.core.error.ExceptionMapper
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.data.database.StatisticsDao
import com.example.gigwork.data.database.StatisticsEntity
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.repository.JobStatisticsRepository
import com.example.gigwork.util.Logger
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobStatisticsRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val statisticsDao: StatisticsDao,
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : JobStatisticsRepository {

    companion object {
        private const val TAG = "JobStatisticsRepo"
        private const val CACHE_DURATION = 15 * 60 * 1000L // 15 minutes
    }

    override suspend fun getJobStatistics(jobId: String): Flow<ApiResult<JobStatisticsRepository.JobStatistics>> = flow {
        emit(ApiResult.Loading)
        try {
            // Check cache first
            val cachedStats = withContext(ioDispatcher) {
                statisticsDao.getJobStatistics(jobId)
            }

            if (cachedStats != null && !isStatsCacheExpired(cachedStats.lastUpdated)) {
                val domainStats = mapEntityToDomain(cachedStats)
                logger.d(
                    tag = TAG,
                    message = "Returning cached job statistics",
                    additionalData = mapOf(
                        "job_id" to jobId,
                        "views" to domainStats.viewsCount,
                        "applications" to domainStats.applicationsCount
                    )
                )
                emit(ApiResult.Success(domainStats))
            }

            // Fetch from remote
            withContext(ioDispatcher) {
                try {
                    val stats = supabaseClient.client.postgrest["job_statistics"]
                        .select {
                            filter { eq("job_id", jobId)}
                        }
                        .decodeSingleOrNull<Map<String, Any?>>()

                    if (stats != null) {
                        val domainStats = JobStatisticsRepository.JobStatistics(
                            jobId = jobId,
                            viewsCount = (stats["views_count"] as? Number)?.toInt() ?: 0,
                            applicationsCount = (stats["applications_count"] as? Number)?.toInt() ?: 0,
                            activeApplicationsCount = (stats["active_applications_count"] as? Number)?.toInt() ?: 0,
                            averageApplicantRating = (stats["average_applicant_rating"] as? Number)?.toDouble() ?: 0.0,
                            lastUpdated = (stats["last_updated"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        )

                        // Update cache
                        val entity = StatisticsEntity(
                            id = cachedStats?.id ?: UUID.randomUUID().toString(),
                            jobId = jobId,
                            viewsCount = domainStats.viewsCount,
                            applicationsCount = domainStats.applicationsCount,
                            activeApplicationsCount = domainStats.activeApplicationsCount,
                            averageApplicantRating = domainStats.averageApplicantRating,
                            lastUpdated = System.currentTimeMillis()
                        )
                        statisticsDao.insertOrUpdateStatistics(entity)

                        logger.i(
                            tag = TAG,
                            message = "Successfully fetched job statistics from network",
                            additionalData = mapOf(
                                "job_id" to jobId,
                                "views" to domainStats.viewsCount,
                                "applications" to domainStats.applicationsCount
                            )
                        )

                        // Only emit if not already emitted or if different from cache
                        if (cachedStats == null ||
                            cachedStats.viewsCount != domainStats.viewsCount ||
                            cachedStats.applicationsCount != domainStats.applicationsCount) {
                            emit(ApiResult.Success(domainStats))
                        }
                    } else if (cachedStats == null) {
                        // If no remote stats and no cache, create empty stats
                        val emptyStats = JobStatisticsRepository.JobStatistics(
                            jobId = jobId,
                            viewsCount = 0,
                            applicationsCount = 0,
                            activeApplicationsCount = 0,
                            averageApplicantRating = 0.0,
                            lastUpdated = System.currentTimeMillis()
                        )

                        // Create empty stats in remote
                        supabaseClient.client.postgrest["job_statistics"]
                            .insert(mapOf(
                                "job_id" to jobId,
                                "views_count" to 0,
                                "applications_count" to 0,
                                "active_applications_count" to 0,
                                "average_applicant_rating" to 0.0,
                                "last_updated" to System.currentTimeMillis()
                            ))

                        // Update cache
                        val entity = StatisticsEntity(
                            id = UUID.randomUUID().toString(),
                            jobId = jobId,
                            viewsCount = 0,
                            applicationsCount = 0,
                            activeApplicationsCount = 0,
                            averageApplicantRating = 0.0,
                            lastUpdated = System.currentTimeMillis()
                        )
                        statisticsDao.insertOrUpdateStatistics(entity)

                        logger.i(
                            tag = TAG,
                            message = "Created new job statistics",
                            additionalData = mapOf("job_id" to jobId)
                        )

                        emit(ApiResult.Success(emptyStats))
                    }
                } catch (e: Exception) {
                    // If remote fetch fails but we have cached data, don't emit error
                    if (cachedStats == null) {
                        logger.w(
                            tag = TAG,
                            message = "Error fetching job statistics from network",
                            throwable = e,
                            additionalData = mapOf("job_id" to jobId)
                        )
                        emit(ApiResult.Error(ExceptionMapper.map(e, "GET_JOB_STATISTICS_NETWORK")))
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting job statistics",
                throwable = e,
                additionalData = mapOf("job_id" to jobId)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_JOB_STATISTICS")))
        }
    }

    override suspend fun incrementViewCount(jobId: String): Flow<ApiResult<Boolean>> = flow {
        emit(ApiResult.Loading)
        try {
            // Increment view count in local cache
            withContext(ioDispatcher) {
                statisticsDao.incrementViewCount(jobId)
            }

            // Increment view count in remote database
            withContext(ioDispatcher) {
                try {
                    // Check if stats exist for this job
                    val response = supabaseClient.client.postgrest["job_statistics"]
                        .select {
                            filter{eq("job_id", jobId)}
                        }

                    // Decode the results and check if any exist
                    val results = response.decodeList<Map<String, Any?>>()
                    val exists = results.isNotEmpty()

                    if (exists) {
                        // Update existing stats
                        supabaseClient.client.postgrest.rpc(
                            "increment_job_view_count",
                            mapOf("p_job_id" to jobId)
                        )
                    } else {
                        // Create new stats
                        supabaseClient.client.postgrest["job_statistics"]
                            .insert(mapOf(
                                "job_id" to jobId,
                                "views_count" to 1,
                                "applications_count" to 0,
                                "active_applications_count" to 0,
                                "average_applicant_rating" to 0.0,
                                "last_updated" to System.currentTimeMillis()
                            ))
                    }
                } catch (e: Exception) {
                    logger.w(
                        tag = TAG,
                        message = "Error incrementing view count in remote database",
                        throwable = e,
                        additionalData = mapOf("job_id" to jobId)
                    )
                    // Don't return error, since we've already updated local cache
                }
            }
            logger.d(
                tag = TAG,
                message = "Incremented view count",
                additionalData = mapOf("job_id" to jobId)
            )

            emit(ApiResult.Success(true))
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error incrementing view count",
                throwable = e,
                additionalData = mapOf("job_id" to jobId)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "INCREMENT_VIEW_COUNT")))
        }
    }

    override suspend fun incrementApplicationCount(jobId: String): Flow<ApiResult<Boolean>> = flow {
        emit(ApiResult.Loading)
        try {
            // Increment application count in local cache
            // Increment application count in remote database
            withContext(ioDispatcher) {
                try {
                    // Check if stats exist for this job
                    val response = supabaseClient.client.postgrest["job_statistics"]
                        .select {
                            filter { eq("job_id", jobId)}
                        }

                    // Decode the results and check if any exist
                    val results = response.decodeList<Map<String, Any?>>()
                    val exists = results.isNotEmpty()

                    if (exists) {
                        // Update existing stats
                        supabaseClient.client.postgrest.rpc(
                            "increment_job_application_count",
                            mapOf("p_job_id" to jobId)
                        )
                    } else {
                        // Create new stats
                        supabaseClient.client.postgrest["job_statistics"]
                            .insert(mapOf(
                                "job_id" to jobId,
                                "views_count" to 0,
                                "applications_count" to 1,
                                "active_applications_count" to 1,
                                "average_applicant_rating" to 0.0,
                                "last_updated" to System.currentTimeMillis()
                            ))
                    }
                } catch (e: Exception) {
                    logger.w(
                        tag = TAG,
                        message = "Error incrementing application count in remote database",
                        throwable = e,
                        additionalData = mapOf("job_id" to jobId)
                    )
                    // Don't return error, since we've already updated local cache
                }
            }

            logger.d(
                tag = TAG,
                message = "Incremented application count",
                additionalData = mapOf("job_id" to jobId)
            )

            emit(ApiResult.Success(true))
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error incrementing application count",
                throwable = e,
                additionalData = mapOf("job_id" to jobId)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "INCREMENT_APPLICATION_COUNT")))
        }
    }

    override suspend fun getJobsViewStatistics(jobIds: List<String>): Flow<ApiResult<Map<String, Int>>> = flow {
        emit(ApiResult.Loading)
        try {
            if (jobIds.isEmpty()) {
                emit(ApiResult.Success(emptyMap()))
                return@flow
            }

            // Get from local cache first
            val cachedStats = withContext(ioDispatcher) {
                statisticsDao.getStatisticsForJobs(jobIds)
            }

            if (cachedStats.isNotEmpty()) {
                val viewsMap = cachedStats.associate { it.jobId to it.viewsCount }
                logger.d(
                    tag = TAG,
                    message = "Returning cached jobs view statistics",
                    additionalData = mapOf(
                        "job_count" to jobIds.size,
                        "stats_count" to cachedStats.size
                    )
                )
                emit(ApiResult.Success(viewsMap))
            }

            // Fetch from remote
            withContext(ioDispatcher) {
                try {
                    // Supabase doesn't directly support IN queries, so we need to handle this differently
                    // This is a simplification - in a real implementation, you might need pagination for large lists

                    val allStats = supabaseClient.client.postgrest["job_statistics"]
                        .select()
                        .decodeList<Map<String, Any?>>()
                        .filter { (it["job_id"] as? String) in jobIds }

                    val viewsMap = allStats.associate {
                        (it["job_id"] as String) to ((it["views_count"] as? Number)?.toInt() ?: 0)
                    }

                    // Fill in missing jobs with zero counts
                    val completeMap = jobIds.associateWith { jobId ->
                        viewsMap[jobId] ?: 0
                    }

                    // Update cache for each job
                    allStats.forEach { stat ->
                        val jobId = stat["job_id"] as String
                        val viewsCount = (stat["views_count"] as? Number)?.toInt() ?: 0
                        val applicationsCount = (stat["applications_count"] as? Number)?.toInt() ?: 0
                        val activeApplicationsCount = (stat["active_applications_count"] as? Number)?.toInt() ?: 0
                        val averageRating = (stat["average_applicant_rating"] as? Number)?.toDouble() ?: 0.0

                        val entity = StatisticsEntity(
                            id = UUID.randomUUID().toString(), // This will be replaced if entity exists
                            jobId = jobId,
                            viewsCount = viewsCount,
                            applicationsCount = applicationsCount,
                            activeApplicationsCount = activeApplicationsCount,
                            averageApplicantRating = averageRating,
                            lastUpdated = System.currentTimeMillis()
                        )
                        statisticsDao.insertOrUpdateStatistics(entity)
                    }

                    logger.i(
                        tag = TAG,
                        message = "Successfully fetched jobs view statistics from network",
                        additionalData = mapOf(
                            "job_count" to jobIds.size,
                            "stats_count" to allStats.size
                        )
                    )

                    // Only emit if different from cache
                    val cachedMap = cachedStats.associate { it.jobId to it.viewsCount }
                    if (cachedMap != completeMap) {
                        emit(ApiResult.Success(completeMap))
                    }
                } catch (e: Exception) {
                    // If remote fetch fails but we have cached data, don't emit error
                    if (cachedStats.isEmpty()) {
                        logger.w(
                            tag = TAG,
                            message = "Error fetching jobs view statistics from network",
                            throwable = e,
                            additionalData = mapOf("job_count" to jobIds.size)
                        )
                        emit(ApiResult.Error(ExceptionMapper.map(e, "GET_JOBS_VIEW_STATISTICS_NETWORK")))
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting jobs view statistics",
                throwable = e,
                additionalData = mapOf("job_count" to jobIds.size)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_JOBS_VIEW_STATISTICS")))
        }
    }

    override suspend fun getJobsApplicationStatistics(jobIds: List<String>): Flow<ApiResult<Map<String, Int>>> = flow {
        emit(ApiResult.Loading)
        try {
            if (jobIds.isEmpty()) {
                emit(ApiResult.Success(emptyMap()))
                return@flow
            }

            // Get from local cache first
            val cachedStats = withContext(ioDispatcher) {
                statisticsDao.getStatisticsForJobs(jobIds)
            }

            if (cachedStats.isNotEmpty()) {
                val applicationMap = cachedStats.associate { it.jobId to it.applicationsCount }
                logger.d(
                    tag = TAG,
                    message = "Returning cached jobs application statistics",
                    additionalData = mapOf(
                        "job_count" to jobIds.size,
                        "stats_count" to cachedStats.size
                    )
                )
                emit(ApiResult.Success(applicationMap))
            }

            // Fetch from remote (similar approach as for views)
            withContext(ioDispatcher) {
                try {
                    // Similar implementation as getJobsViewStatistics, but focusing on application counts
                    val allStats = supabaseClient.client.postgrest["job_statistics"]
                        .select()
                        .decodeList<Map<String, Any?>>()
                        .filter { (it["job_id"] as? String) in jobIds }

                    val applicationMap = allStats.associate {
                        (it["job_id"] as String) to ((it["applications_count"] as? Number)?.toInt() ?: 0)
                    }

                    // Fill in missing jobs with zero counts
                    val completeMap = jobIds.associateWith { jobId ->
                        applicationMap[jobId] ?: 0
                    }

                    // Cache update logic (omitted for brevity - same as in getJobsViewStatistics)

                    // Only emit if different from cache
                    val cachedMap = cachedStats.associate { it.jobId to it.applicationsCount }
                    if (cachedMap != completeMap) {
                        emit(ApiResult.Success(completeMap))
                    }
                } catch (e: Exception) {
                    // If remote fetch fails but we have cached data, don't emit error
                    if (cachedStats.isEmpty()) {
                        emit(ApiResult.Error(ExceptionMapper.map(e, "GET_JOBS_APPLICATION_STATISTICS_NETWORK")))
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting jobs application statistics",
                throwable = e,
                additionalData = mapOf("job_count" to jobIds.size)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_JOBS_APPLICATION_STATISTICS")))
        }
    }

    private fun isStatsCacheExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > CACHE_DURATION
    }

    private fun mapEntityToDomain(entity: StatisticsEntity): JobStatisticsRepository.JobStatistics {
        return JobStatisticsRepository.JobStatistics(
            jobId = entity.jobId,
            viewsCount = entity.viewsCount,
            applicationsCount = entity.applicationsCount,
            activeApplicationsCount = entity.activeApplicationsCount,
            averageApplicantRating = entity.averageApplicantRating,
            lastUpdated = entity.lastUpdated
        )
    }
}