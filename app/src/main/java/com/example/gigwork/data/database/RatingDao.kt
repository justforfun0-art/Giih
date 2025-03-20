package com.example.gigwork.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapInfo
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import com.example.gigwork.domain.repository.EmployerRatingRepository.EmployerRating

/**
 * Data Access Object for rating-related operations
 * Handles both employer ratings and employer rating summaries
 */
@Dao
interface RatingDao {
    // ===== EMPLOYER RATING SUMMARY METHODS =====

    /**
     * Get employer rating summary
     */
    @Query("SELECT * FROM employer_rating_summaries WHERE employerId = :employerId")
    suspend fun getEmployerRatingSummary(employerId: String): EmployerRatingSummary?

    /**
     * Insert employer rating summary
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployerRatingSummary(summary: EmployerRatingSummary)

    /**
     * Delete employer rating summary
     */
    @Query("DELETE FROM employer_rating_summaries WHERE employerId = :employerId")
    suspend fun deleteEmployerRatingSummary(employerId: String)

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

    // ===== EMPLOYER RATING METHODS =====

    /**
     * Insert a new employer rating
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRating(rating: RatingEntity)

    /**
     * Insert multiple ratings
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRatings(ratings: List<RatingEntity>)

    /**
     * Update an existing rating
     */
    @Update
    suspend fun updateRating(rating: RatingEntity)

    /**
     * Get rating by ID
     */
    @Query("SELECT * FROM employer_ratings WHERE id = :ratingId")
    suspend fun getRatingById(ratingId: String): RatingEntity?

    /**
     * Get all ratings for an employer
     */
    @Query("SELECT * FROM employer_ratings WHERE employerId = :employerId ORDER BY timestamp DESC")
    suspend fun getRatingsByEmployerId(employerId: String): List<RatingEntity>

    /**
     * Get all ratings by a user
     */
    @Query("SELECT * FROM employer_ratings WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getRatingsByUserId(userId: String): List<RatingEntity>

    /**
     * Get average rating for an employer
     */
    @Query("SELECT AVG(rating) FROM employer_ratings WHERE employerId = :employerId")
    suspend fun getAverageRatingForEmployer(employerId: String): Double

    /**
     * Delete a rating
     */
    @Query("DELETE FROM employer_ratings WHERE id = :ratingId")
    suspend fun deleteRating(ratingId: String)

    /**
     * Check if a user has rated an employer
     */
    @Query("SELECT EXISTS(SELECT 1 FROM employer_ratings WHERE userId = :userId AND employerId = :employerId LIMIT 1)")
    suspend fun hasUserRatedEmployer(userId: String, employerId: String): Boolean

    /**
     * Get user's rating for an employer
     */
    @Query("SELECT * FROM employer_ratings WHERE userId = :userId AND employerId = :employerId LIMIT 1")
    suspend fun getUserRatingForEmployer(userId: String, employerId: String): RatingEntity?

    /**
     * Get rating distribution for an employer
     */
    @Query("""
    SELECT 
        CAST(rating AS INTEGER) as ratingValue, 
        CAST(COUNT(*) AS INTEGER) as ratingCount 
    FROM employer_ratings 
    WHERE employerId = :employerId 
    GROUP BY CAST(rating AS INTEGER)
""")
    @MapInfo(keyColumn = "ratingValue", valueColumn = "ratingCount")
    suspend fun getRatingDistributionForEmployer(employerId: String): Map<Int, Int>
    /**
     * Insert or update rating
     */
    @Transaction
    suspend fun insertOrUpdateRating(rating: RatingEntity) {
        val existing = getRatingById(rating.id)
        if (existing == null) {
            insertRating(rating)
        } else {
            updateRating(rating)
        }
    }

    /**
     * Delete ratings for an employer
     */
    @Query("DELETE FROM employer_ratings WHERE employerId = :employerId")
    suspend fun deleteRatingsForEmployer(employerId: String)

    /**
     * Delete ratings by a user
     */
    @Query("DELETE FROM employer_ratings WHERE userId = :userId")
    suspend fun deleteUserRatings(userId: String)

    /**
     * Get employers with highest average ratings
     */
    @RewriteQueriesToDropUnusedColumns
    @Query("""
    SELECT
        employerId,
        AVG(rating) as rating,
        COUNT(*) as totalRatings,
        0.0 as responseRate,
        0 as averageResponseTime,
        '{}' as ratings
    FROM employer_ratings
    GROUP BY employerId
    ORDER BY rating DESC
    LIMIT :limit
""")


    suspend fun getTopRatedEmployers(limit: Int): List<EmployerRating>

    /**
     * Count ratings for an employer by rating value
     */
    @Query("""
    SELECT 
        CAST(rating AS INTEGER) as ratingValue, 
        CAST(COUNT(*) AS INTEGER) as ratingCount 
    FROM employer_ratings 
    WHERE employerId = :employerId 
    GROUP BY CAST(rating AS INTEGER)
""")
    @MapInfo(keyColumn = "ratingValue", valueColumn = "ratingCount")
    suspend fun countRatingsByValue(employerId: String): Map<Int, Int>
    // ===== JOB RATING METHODS =====

    /**
     * Insert a new job rating
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobRating(rating: JobRatingEntity)

    /**
     * Get all ratings for a job
     */
    @Query("SELECT * FROM job_ratings WHERE jobId = :jobId ORDER BY timestamp DESC")
    suspend fun getRatingsByJobId(jobId: String): List<JobRatingEntity>

    /**
     * Get average rating for a job
     */
    @Query("SELECT AVG(rating) FROM job_ratings WHERE jobId = :jobId")
    suspend fun getAverageRatingForJob(jobId: String): Double

    /**
     * Get user's rating for a job
     */
    @Query("SELECT * FROM job_ratings WHERE userId = :userId AND jobId = :jobId")
    suspend fun getUserRatingForJob(userId: String, jobId: String): JobRatingEntity?

    /**
     * Get jobs rated by a user
     */
    @Query("SELECT jobId FROM job_ratings WHERE userId = :userId")
    suspend fun getJobsRatedByUser(userId: String): List<String>
}