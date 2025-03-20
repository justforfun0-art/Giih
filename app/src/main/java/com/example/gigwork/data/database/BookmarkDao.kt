// BookmarkDao.kt
package com.example.gigwork.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapInfo
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BookmarkDao {
    /**
     * Insert a new bookmark
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    /**
     * Delete a specific bookmark
     */
    @Query("DELETE FROM bookmarks WHERE userId = :userId AND jobId = :jobId")
    suspend fun deleteBookmark(userId: String, jobId: String)

    /**
     * Check if a job is bookmarked by a user
     */
    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE userId = :userId AND jobId = :jobId LIMIT 1)")
    suspend fun isJobBookmarked(userId: String, jobId: String): Boolean

    /**
     * Get all bookmarks for a user
     */
    @Query("SELECT * FROM bookmarks WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getUserBookmarks(userId: String): List<BookmarkEntity>

    /**
     * Get bookmark count for a specific job
     */
    @Query("SELECT COUNT(*) FROM bookmarks WHERE jobId = :jobId")
    suspend fun getBookmarkCount(jobId: String): Int

    /**
     * Get bookmark counts for multiple jobs
     */
    // In BookmarkDao.kt
    @Query("SELECT jobId, COUNT(*) as count FROM bookmarks WHERE jobId IN (:jobIds) GROUP BY jobId")
    @MapInfo(keyColumn = "jobId", valueColumn = "count")
    suspend fun getBookmarkCountsForJobs(jobIds: List<String>): Map<String, Int>

    /**
     * Clear all bookmarks for a user
     */
    @Query("DELETE FROM bookmarks WHERE userId = :userId")
    suspend fun clearUserBookmarks(userId: String)

    /**
     * Get all users who bookmarked a specific job
     */
    @Query("SELECT userId FROM bookmarks WHERE jobId = :jobId")
    suspend fun getUsersWhoBookmarkedJob(jobId: String): List<String>

    /**
     * Get all job IDs bookmarked by a user
     */
    @Query("SELECT jobId FROM bookmarks WHERE userId = :userId")
    suspend fun getBookmarkedJobIds(userId: String): List<String>

    /**
     * Delete old bookmarks
     */
    @Query("DELETE FROM bookmarks WHERE timestamp < :timestamp")
    suspend fun deleteOldBookmarks(timestamp: Long)
}