// StatisticsDao.kt
package com.example.gigwork.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface StatisticsDao {
    /**
     * Insert or update job statistics
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStatistics(statistics: StatisticsEntity)

    /**
     * Get statistics for a job
     */
    @Query("SELECT * FROM job_statistics WHERE jobId = :jobId")
    suspend fun getJobStatistics(jobId: String): StatisticsEntity?

    /**
     * Get statistics for multiple jobs
     */
    @Query("SELECT * FROM job_statistics WHERE jobId IN (:jobIds)")
    suspend fun getStatisticsForJobs(jobIds: List<String>): List<StatisticsEntity>

    /**
     * Increment view count for a job
     */
    @Query("UPDATE job_statistics SET viewsCount = viewsCount + 1, lastUpdated = :timestamp WHERE jobId = :jobId")
    suspend fun updateViewCount(jobId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Increment application count for a job
     */
    @Query("UPDATE job_statistics SET applicationsCount = applicationsCount + 1, lastUpdated = :timestamp WHERE jobId = :jobId")
    suspend fun updateApplicationCount(jobId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Delete statistics for a job
     */
    @Query("DELETE FROM job_statistics WHERE jobId = :jobId")
    suspend fun deleteJobStatistics(jobId: String)

    /**
     * Increment view count for a job
     * If no record exists, create one
     */
    @Transaction
    suspend fun incrementViewCount(jobId: String) {
        val stats = getJobStatistics(jobId)
        if (stats == null) {
            val newStats = StatisticsEntity(
                id = java.util.UUID.randomUUID().toString(),
                jobId = jobId,
                viewsCount = 1,
                applicationsCount = 0,
                activeApplicationsCount = 0,
                averageApplicantRating = 0.0,
                lastUpdated = System.currentTimeMillis()
            )
            insertOrUpdateStatistics(newStats)
        } else {
            updateViewCount(jobId)
        }
    }

    /**
     * Increment application count for a job
     * If no record exists, create one
     */
    @Transaction
    suspend fun incrementApplicationCount(jobId: String) {
        val stats = getJobStatistics(jobId)
        if (stats == null) {
            val newStats = StatisticsEntity(
                id = java.util.UUID.randomUUID().toString(),
                jobId = jobId,
                viewsCount = 0,
                applicationsCount = 1,
                activeApplicationsCount = 1,
                averageApplicantRating = 0.0,
                lastUpdated = System.currentTimeMillis()
            )
            insertOrUpdateStatistics(newStats)
        } else {
            updateApplicationCount(jobId)
        }
    }

    /**
     * Increment active application count
     */
    @Query("UPDATE job_statistics SET activeApplicationsCount = activeApplicationsCount + 1, lastUpdated = :timestamp WHERE jobId = :jobId")
    suspend fun incrementActiveApplicationCount(jobId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Decrement active application count
     */
    @Query("UPDATE job_statistics SET activeApplicationsCount = MAX(0, activeApplicationsCount - 1), lastUpdated = :timestamp WHERE jobId = :jobId")
    suspend fun decrementActiveApplicationCount(jobId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Update average applicant rating
     */
    @Query("UPDATE job_statistics SET averageApplicantRating = :rating, lastUpdated = :timestamp WHERE jobId = :jobId")
    suspend fun updateAverageApplicantRating(jobId: String, rating: Double, timestamp: Long = System.currentTimeMillis())

    /**
     * Get jobs with the most views
     */
    @Query("SELECT * FROM job_statistics ORDER BY viewsCount DESC LIMIT :limit")
    suspend fun getMostViewedJobs(limit: Int): List<StatisticsEntity>

    /**
     * Get jobs with the most applications
     */
    @Query("SELECT * FROM job_statistics ORDER BY applicationsCount DESC LIMIT :limit")
    suspend fun getMostAppliedJobs(limit: Int): List<StatisticsEntity>

    /**
     * Insert or update employer rating summary
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployerRatingSummary(summary: EmployerRatingSummary)

    /**
     * Convenience method for inserting employer rating summary
     */
    @Transaction
    suspend fun insertEmployerRatingSummary(
        employerId: String,
        averageRating: Double,
        totalRatings: Int,
        responseRate: Double,
        averageResponseTime: Long,
        distribution: Map<Int, Int>,
        timestamp: Long
    ) {
        val summary = EmployerRatingSummary(
            employerId = employerId,
            averageRating = averageRating,
            totalRatings = totalRatings,
            responseRate = responseRate,
            averageResponseTime = averageResponseTime,
            distribution = distribution,
            timestamp = timestamp
        )
        insertEmployerRatingSummary(summary)
    }

    /**
     * Get employer rating summary
     */
    @Query("SELECT * FROM employer_rating_summaries WHERE employerId = :employerId")
    suspend fun getEmployerRatingSummary(employerId: String): EmployerRatingSummary?

    /**
     * Delete employer rating summary
     */
    @Query("DELETE FROM employer_rating_summaries WHERE employerId = :employerId")
    suspend fun deleteEmployerRatingSummary(employerId: String)
}