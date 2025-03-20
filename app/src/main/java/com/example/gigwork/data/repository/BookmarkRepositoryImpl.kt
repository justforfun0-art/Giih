package com.example.gigwork.data.repository

import com.example.gigwork.core.error.ExceptionMapper
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.data.database.BookmarkDao
import com.example.gigwork.data.database.BookmarkEntity
import com.example.gigwork.data.database.dao.JobDao
import com.example.gigwork.data.database.toDomain
import com.example.gigwork.data.database.toEntity
import com.example.gigwork.data.mappers.toDomain
import com.example.gigwork.data.models.JobDto
import com.example.gigwork.data.security.EncryptedPreferences
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.repository.BookmarkRepository
import com.example.gigwork.util.Logger
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val bookmarkDao: BookmarkDao,
    private val jobDao: JobDao,
    private val encryptedPreferences: EncryptedPreferences,
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BookmarkRepository {

    companion object {
        private const val TAG = "BookmarkRepository"
        private const val CACHE_DURATION = 30 * 60 * 1000L // 30 minutes
    }

    override suspend fun addBookmark(jobId: String): Flow<ApiResult<Boolean>> = flow {
        emit(ApiResult.Loading)
        try {
            val userId = getCurrentUserId()
            if (userId.isBlank()) {
                emit(ApiResult.Error(ExceptionMapper.map(IllegalStateException("User not logged in"), "ADD_BOOKMARK")))
                return@flow
            }

            // Check if already bookmarked
            val isAlreadyBookmarked = withContext(ioDispatcher) {
                bookmarkDao.isJobBookmarked(userId, jobId)
            }

            if (isAlreadyBookmarked) {
                logger.d(
                    tag = TAG,
                    message = "Job already bookmarked",
                    additionalData = mapOf(
                        "user_id" to userId,
                        "job_id" to jobId
                    )
                )
                emit(ApiResult.Success(true))
                return@flow
            }

            // Add to remote database
            withContext(ioDispatcher) {
                val bookmark = mapOf(
                    "user_id" to userId,
                    "job_id" to jobId,
                    "created_at" to System.currentTimeMillis()
                )

                supabaseClient.client.postgrest["bookmarks"]
                    .insert(bookmark)
            }

            // Add to local database
            val entity = BookmarkEntity(
                id = 0, // Auto-generated
                userId = userId,
                jobId = jobId,
                timestamp = System.currentTimeMillis()
            )
            bookmarkDao.insertBookmark(entity)

            logger.i(
                tag = TAG,
                message = "Successfully added bookmark",
                additionalData = mapOf(
                    "user_id" to userId,
                    "job_id" to jobId
                )
            )

            emit(ApiResult.Success(true))
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error adding bookmark",
                throwable = e,
                additionalData = mapOf("job_id" to jobId)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "ADD_BOOKMARK")))
        }
    }

    override suspend fun removeBookmark(jobId: String): Flow<ApiResult<Boolean>> = flow {
        emit(ApiResult.Loading)
        try {
            val userId = getCurrentUserId()
            if (userId.isBlank()) {
                emit(ApiResult.Error(ExceptionMapper.map(IllegalStateException("User not logged in"), "REMOVE_BOOKMARK")))
                return@flow
            }

            // Remove from remote database
            withContext(ioDispatcher) {
                supabaseClient.client.postgrest["bookmarks"]
                    .delete {
                        filter { eq("user_id", userId)}
                       filter {  eq("job_id", jobId)}
                    }
            }

            // Remove from local database
            bookmarkDao.deleteBookmark(userId, jobId)

            logger.i(
                tag = TAG,
                message = "Successfully removed bookmark",
                additionalData = mapOf(
                    "user_id" to userId,
                    "job_id" to jobId
                )
            )

            emit(ApiResult.Success(true))
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error removing bookmark",
                throwable = e,
                additionalData = mapOf("job_id" to jobId)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "REMOVE_BOOKMARK")))
        }
    }

    override suspend fun isJobBookmarked(jobId: String): Flow<ApiResult<Boolean>> = flow {
        emit(ApiResult.Loading)
        try {
            val userId = getCurrentUserId()
            if (userId.isBlank()) {
                emit(ApiResult.Error(ExceptionMapper.map(IllegalStateException("User not logged in"), "IS_JOB_BOOKMARKED")))
                return@flow
            }

            // Check local database first
            val isBookmarked = withContext(ioDispatcher) {
                bookmarkDao.isJobBookmarked(userId, jobId)
            }

            // If found in local database, return result
            if (isBookmarked) {
                emit(ApiResult.Success(true))
                return@flow
            }

            // If not found, check remote database
            val remoteResult = withContext(ioDispatcher) {
                try {
                    val response = supabaseClient.client.postgrest["bookmarks"]
                        .select {
                            filter { eq("user_id", userId)}
                            filter { eq("job_id", jobId)}
                        }

                    // Check if any results were returned
                    val results = response.decodeList<Map<String, Any?>>()
                    results.isNotEmpty()
                } catch (e: Exception) {
                    // If remote check fails, trust the local result
                    logger.w(
                        tag = TAG,
                        message = "Remote bookmark check failed, using local result",
                        throwable = e,
                        additionalData = mapOf(
                            "user_id" to userId,
                            "job_id" to jobId
                        )
                    )
                    isBookmarked
                }
            }

            // Sync local database if different
            if (remoteResult && !isBookmarked) {
                val entity = BookmarkEntity(
                    id = 0, // Auto-generated
                    userId = userId,
                    jobId = jobId,
                    timestamp = System.currentTimeMillis()
                )
                bookmarkDao.insertBookmark(entity)
            }

            emit(ApiResult.Success(remoteResult))
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error checking if job is bookmarked",
                throwable = e,
                additionalData = mapOf("job_id" to jobId)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "IS_JOB_BOOKMARKED")))
        }
    }

    override suspend fun getBookmarkedJobs(): Flow<ApiResult<List<Job>>> = flow {
        emit(ApiResult.Loading)
        try {
            val userId = getCurrentUserId()
            if (userId.isBlank()) {
                emit(ApiResult.Error(ExceptionMapper.map(IllegalStateException("User not logged in"), "GET_BOOKMARKED_JOBS")))
                return@flow
            }

            // Get from local cache first
            val cachedBookmarks = withContext(ioDispatcher) {
                bookmarkDao.getUserBookmarks(userId)
            }

            if (cachedBookmarks.isNotEmpty()) {
                val cachedJobIds = cachedBookmarks.map { it.jobId }
                // Use a query that fetches multiple jobs by their IDs
                val cachedJobs = withContext(ioDispatcher) {
                    jobDao.getJobsByIds(cachedJobIds)
                }

                if (cachedJobs.isNotEmpty()) {
                    logger.d(
                        tag = TAG,
                        message = "Returning cached bookmarked jobs",
                        additionalData = mapOf(
                            "user_id" to userId,
                            "job_count" to cachedJobs.size
                        )
                    )
                    // Use explicit typing to resolve toDomain() ambiguity
                    val domainJobs = cachedJobs.map { jobEntity -> jobEntity.toDomain() }
                    emit(ApiResult.Success(domainJobs))
                }
            }

            // Fetch from remote
            withContext(ioDispatcher) {
                try {
                    // Get bookmarked job IDs
                    val bookmarks = supabaseClient.client.postgrest["bookmarks"]
                        .select {
                            filter { eq("user_id", userId)}
                        }
                        .decodeList<Map<String, Any?>>()

                    val jobIds = bookmarks.mapNotNull { it["job_id"] as? String }

                    if (jobIds.isEmpty()) {
                        if (cachedBookmarks.isEmpty()) {
                            emit(ApiResult.Success(emptyList()))
                        }
                        return@withContext
                    }

                    // Fetch jobs by IDs
                    val jobs = mutableListOf<Job>()
                    for (jobId in jobIds) {
                        try {
                            val job = supabaseClient.client.postgrest["jobs"]
                                .select {
                                    filter { eq("id", jobId)}
                                }
                                .decodeSingleOrNull<JobDto>()

                            if (job != null) {
                                // Use explicit JobDto.toDomain() to resolve ambiguity
                                val domainJob = job.toDomain()
                                jobs.add(domainJob)
                                // Update job cache
                                jobDao.insertJob(domainJob.toEntity())
                            }
                        } catch (e: Exception) {
                            logger.w(
                                tag = TAG,
                                message = "Error fetching job details",
                                throwable = e,
                                additionalData = mapOf("job_id" to jobId)
                            )
                        }
                    }

                    // Update bookmark cache
                    bookmarkDao.clearUserBookmarks(userId)
                    bookmarks.forEach {
                        val jobId = it["job_id"] as? String ?: return@forEach
                        val timestamp = (it["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()

                        val entity = BookmarkEntity(
                            id = 0, // Auto-generated
                            userId = userId,
                            jobId = jobId,
                            timestamp = timestamp
                        )
                        bookmarkDao.insertBookmark(entity)
                    }

                    logger.i(
                        tag = TAG,
                        message = "Successfully fetched bookmarked jobs from network",
                        additionalData = mapOf(
                            "user_id" to userId,
                            "job_count" to jobs.size
                        )
                    )

                    emit(ApiResult.Success(jobs))
                } catch (e: Exception) {
                    // If remote fetch fails but we have cached data, don't emit error
                    if (cachedBookmarks.isEmpty()) {
                        emit(ApiResult.Error(ExceptionMapper.map(e, "GET_BOOKMARKED_JOBS_NETWORK")))
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting bookmarked jobs",
                throwable = e
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_BOOKMARKED_JOBS")))
        }
    }

    override suspend fun getBookmarkCount(jobId: String): Flow<ApiResult<Int>> = flow {
        emit(ApiResult.Loading)
        try {
            // Get from local cache first
            val localCount = withContext(ioDispatcher) {
                bookmarkDao.getBookmarkCount(jobId)
            }

            emit(ApiResult.Success(localCount))

            // Fetch from remote to update cache
            withContext(ioDispatcher) {
                try {
                    val response = supabaseClient.client.postgrest["bookmarks"]
                        .select {
                           filter {  eq("job_id", jobId)}
                        }

                    // Decode the list and count the results
                    val results = response.decodeList<Map<String, Any?>>()
                    val remoteCount = results.size

                    // Only emit if different from local count
                    if (remoteCount != localCount) {
                        emit(ApiResult.Success(remoteCount))
                    }

                    logger.d(
                        tag = TAG,
                        message = "Fetched bookmark count from network",
                        additionalData = mapOf(
                            "job_id" to jobId,
                            "count" to remoteCount
                        )
                    )
                } catch (e: Exception) {
                    // Just log warning, since we already emitted the local count
                    logger.w(
                        tag = TAG,
                        message = "Error fetching bookmark count from network",
                        throwable = e,
                        additionalData = mapOf("job_id" to jobId)
                    )
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting bookmark count",
                throwable = e,
                additionalData = mapOf("job_id" to jobId)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_BOOKMARK_COUNT")))
        }
    }

    private fun getCurrentUserId(): String {
        return encryptedPreferences.getUserId()
    }
}