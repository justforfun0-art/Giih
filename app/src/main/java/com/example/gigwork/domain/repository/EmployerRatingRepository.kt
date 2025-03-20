package com.example.gigwork.domain.repository

import com.example.gigwork.core.result.ApiResult
import kotlinx.coroutines.flow.Flow

interface EmployerRatingRepository {
    /**
     * Get employer rating information
     * @param employerId Unique identifier of the employer
     * @return Flow of ApiResult containing employer rating data
     */
    suspend fun getEmployerRating(employerId: String): Flow<ApiResult<EmployerRating>>

    /**
     * Submit a new rating for an employer
     * @param employerId Unique identifier of the employer
     * @param rating Rating value
     * @param review Optional review text
     * @return Flow of ApiResult indicating success or failure
     */
    suspend fun submitRating(
        employerId: String,
        rating: Float,
        review: String? = null
    ): Flow<ApiResult<Boolean>>

    /**
     * Get average rating for an employer
     * @param employerId Unique identifier of the employer
     * @return Flow of ApiResult containing the average rating
     */
    suspend fun getAverageRating(employerId: String): Flow<ApiResult<Float>>

    /**
     * Get all ratings for an employer
     * @param employerId Unique identifier of the employer
     * @return Flow of ApiResult containing list of ratings
     */
    suspend fun getEmployerRatings(employerId: String): Flow<ApiResult<List<Rating>>>

    data class EmployerRating(
        val rating: Double,
        val totalRatings: Int,
        val responseRate: Double,
        val averageResponseTime: Long,
        val ratings: Map<Int, Int> = emptyMap() // Distribution of ratings (1-5 stars)
    )

    data class Rating(
        val rating: Float,
        val review: String?,
        val userId: String,
        val timestamp: Long
    )
}