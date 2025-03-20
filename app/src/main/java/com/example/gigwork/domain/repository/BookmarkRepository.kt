package com.example.gigwork.domain.repository

import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.Job
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    /**
     * Add a job to user's bookmarks
     * @param jobId Unique identifier of the job to bookmark
     * @return Flow of ApiResult indicating success or failure
     */
    suspend fun addBookmark(jobId: String): Flow<ApiResult<Boolean>>

    /**
     * Remove a job from user's bookmarks
     * @param jobId Unique identifier of the job to remove
     * @return Flow of ApiResult indicating success or failure
     */
    suspend fun removeBookmark(jobId: String): Flow<ApiResult<Boolean>>

    /**
     * Check if a job is bookmarked by the current user
     * @param jobId Unique identifier of the job to check
     * @return Flow of ApiResult containing bookmark status
     */
    suspend fun isJobBookmarked(jobId: String): Flow<ApiResult<Boolean>>

    /**
     * Get all bookmarked jobs for the current user
     * @return Flow of ApiResult containing list of bookmarked jobs
     */
    suspend fun getBookmarkedJobs(): Flow<ApiResult<List<Job>>>

    /**
     * Get bookmark count for a specific job
     * @param jobId Unique identifier of the job
     * @return Flow of ApiResult containing the bookmark count
     */
    suspend fun getBookmarkCount(jobId: String): Flow<ApiResult<Int>>
}