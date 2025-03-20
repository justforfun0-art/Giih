package com.example.gigwork.data.repository

import com.example.gigwork.core.error.ExceptionMapper
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.data.database.RatingDao
import com.example.gigwork.data.database.RatingEntity
import com.example.gigwork.data.security.EncryptedPreferences
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.repository.EmployerRatingRepository
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
class EmployerRatingRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val ratingDao: RatingDao,
    private val encryptedPreferences: EncryptedPreferences,
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : EmployerRatingRepository {

    companion object {
        private const val TAG = "EmployerRatingRepo"
        private const val CACHE_DURATION = 24 * 60 * 60 * 1000L // 24 hours
    }

    override suspend fun getEmployerRating(employerId: String): Flow<ApiResult<EmployerRatingRepository.EmployerRating>> = flow {
        emit(ApiResult.Loading)
        try {
            // Get from local cache first
            val cachedRating = withContext(ioDispatcher) {
                ratingDao.getEmployerRatingSummary(employerId)
            }

            if (cachedRating != null && !isRatingCacheExpired(cachedRating.timestamp)) {
                val domainRating = EmployerRatingRepository.EmployerRating(
                    rating = cachedRating.averageRating,
                    totalRatings = cachedRating.totalRatings,
                    responseRate = cachedRating.responseRate,
                    averageResponseTime = cachedRating.averageResponseTime,
                    ratings = cachedRating.distribution
                )

                logger.d(
                    tag = TAG,
                    message = "Returning cached employer rating",
                    additionalData = mapOf(
                        "employer_id" to employerId,
                        "rating" to cachedRating.averageRating,
                        "total_ratings" to cachedRating.totalRatings
                    )
                )

                emit(ApiResult.Success(domainRating))
            }

            // Fetch from remote
            withContext(ioDispatcher) {
                try {
                    // Get employer rating summary
                    val response = supabaseClient.client.postgrest.rpc(
                        "get_employer_rating_summary",
                        mapOf("p_employer_id" to employerId)
                    )

                    val summaryMap = response.decodeAs<Map<String, Any?>>()

                    val ratingValue = (summaryMap["average_rating"] as? Number)?.toDouble() ?: 0.0
                    val totalRatings = (summaryMap["total_ratings"] as? Number)?.toInt() ?: 0
                    val responseRate = (summaryMap["response_rate"] as? Number)?.toDouble() ?: 0.0
                    val avgResponseTime = (summaryMap["avg_response_time"] as? Number)?.toLong() ?: 0L

                    // Get rating distribution
                    val distributionResponse = supabaseClient.client.postgrest.rpc(
                        "get_employer_rating_distribution",
                        mapOf("p_employer_id" to employerId)
                    )

                    val distributionList = distributionResponse.decodeAs<List<Map<String, Any?>>>()

                    val distribution = distributionList.associate { item ->
                        val rating = (item["rating"] as? Number)?.toInt() ?: 0
                        val count = (item["count"] as? Number)?.toInt() ?: 0
                        rating to count
                    }

                    // Create complete distribution (1-5 stars)
                    val completeDistribution = (1..5).associateWith { rating ->
                        distribution[rating] ?: 0
                    }

                    val domainRating = EmployerRatingRepository.EmployerRating(
                        rating = ratingValue,
                        totalRatings = totalRatings,
                        responseRate = responseRate,
                        averageResponseTime = avgResponseTime,
                        ratings = completeDistribution
                    )

                    // Update cache
                    ratingDao.insertEmployerRatingSummary(
                        employerId = employerId,
                        averageRating = ratingValue,
                        totalRatings = totalRatings,
                        responseRate = responseRate,
                        averageResponseTime = avgResponseTime,
                        distribution = completeDistribution,
                        timestamp = System.currentTimeMillis()
                    )

                    logger.i(
                        tag = TAG,
                        message = "Successfully fetched employer rating from network",
                        additionalData = mapOf(
                            "employer_id" to employerId,
                            "rating" to ratingValue,
                            "total_ratings" to totalRatings
                        )
                    )

                    // Only emit if different from cache or if cache doesn't exist
                    if (cachedRating == null ||
                        cachedRating.averageRating != ratingValue ||
                        cachedRating.totalRatings != totalRatings) {
                        emit(ApiResult.Success(domainRating))
                    }
                } catch (e: Exception) {
                    // If remote fetch fails but we have cached data, don't emit error
                    if (cachedRating == null) {
                        // If no ratings exist yet, return empty rating
                        val emptyRating = EmployerRatingRepository.EmployerRating(
                            rating = 0.0,
                            totalRatings = 0,
                            responseRate = 0.0,
                            averageResponseTime = 0L,
                            ratings = (1..5).associateWith { 0 }
                        )

                        emit(ApiResult.Success(emptyRating))
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting employer rating",
                throwable = e,
                additionalData = mapOf("employer_id" to employerId)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_EMPLOYER_RATING")))
        }
    }

    override suspend fun submitRating(
        employerId: String,
        rating: Float,
        review: String?
    ): Flow<ApiResult<Boolean>> = flow {
        emit(ApiResult.Loading)
        try {
            val userId = getCurrentUserId()
            if (userId.isBlank()) {
                emit(ApiResult.Error(ExceptionMapper.map(IllegalStateException("User not logged in"), "SUBMIT_RATING")))
                return@flow
            }

            // Validate rating
            if (rating < 1.0f || rating > 5.0f) {
                emit(ApiResult.Error(ExceptionMapper.map(IllegalArgumentException("Rating must be between 1 and 5"), "SUBMIT_RATING_INVALID")))
                return@flow
            }

            // Check if user has already rated this employer
            val existingRating = withContext(ioDispatcher) {
                ratingDao.getUserRatingForEmployer(userId, employerId)
            }

            // Generate rating ID
            val ratingId = existingRating?.id ?: UUID.randomUUID().toString()

            // Submit to remote database
            withContext(ioDispatcher) {
                val payload = mapOf(
                    "id" to ratingId,
                    "user_id" to userId,
                    "employer_id" to employerId,
                    "rating" to rating.toDouble(),
                    "review" to (review ?: ""),
                    "timestamp" to System.currentTimeMillis()
                )

                if (existingRating == null) {
                    // Insert new rating
                    supabaseClient.client.postgrest["employer_ratings"]
                        .insert(payload)
                } else {
                    // Update existing rating
                    supabaseClient.client.postgrest["employer_ratings"]
                        .update(payload) {
                            filter { eq("id", ratingId)}
                        }
                }
            }

            // Update local database
            val entity = RatingEntity(
                id = ratingId,
                userId = userId,
                employerId = employerId,
                rating = rating,
                review = review ?: "",
                timestamp = System.currentTimeMillis()
            )

            if (existingRating == null) {
                ratingDao.insertRating(entity)
            } else {
                ratingDao.updateRating(entity)
            }

            // Invalidate employer rating cache
            ratingDao.deleteEmployerRatingSummary(employerId)

            logger.i(
                tag = TAG,
                message = "Successfully submitted employer rating",
                additionalData = mapOf(
                    "employer_id" to employerId,
                    "user_id" to userId,
                    "rating" to rating,
                    "is_update" to (existingRating != null)
                )
            )

            emit(ApiResult.Success(true))
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error submitting employer rating",
                throwable = e,
                additionalData = mapOf(
                    "employer_id" to employerId,
                    "rating" to rating
                )
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "SUBMIT_RATING")))
        }
    }

    override suspend fun getAverageRating(employerId: String): Flow<ApiResult<Float>> = flow {
        emit(ApiResult.Loading)
        try {
            // Get from local cache first
            val cachedRating = withContext(ioDispatcher) {
                ratingDao.getEmployerRatingSummary(employerId)
            }

            if (cachedRating != null && !isRatingCacheExpired(cachedRating.timestamp)) {
                logger.d(
                    tag = TAG,
                    message = "Returning cached average rating",
                    additionalData = mapOf(
                        "employer_id" to employerId,
                        "rating" to cachedRating.averageRating
                    )
                )
                emit(ApiResult.Success(cachedRating.averageRating.toFloat()))
            }

            // Fetch from remote
            withContext(ioDispatcher) {
                try {
                    val response = supabaseClient.client.postgrest.rpc(
                        "get_employer_average_rating",
                        mapOf("p_employer_id" to employerId)
                    )

                    val resultMap = response.decodeAs<Map<String, Any?>>()
                    val average = (resultMap["average_rating"] as? Number)?.toFloat() ?: 0.0f

                    // Only emit if different from cache or if cache doesn't exist
                    if (cachedRating == null || cachedRating.averageRating.toFloat() != average) {
                        emit(ApiResult.Success(average))
                    }
                } catch (e: Exception) {
                    // If remote fetch fails but we have cached data, don't emit error
                    if (cachedRating == null) {
                        emit(ApiResult.Success(0.0f))
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting average rating",
                throwable = e,
                additionalData = mapOf("employer_id" to employerId)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_AVERAGE_RATING")))
        }
    }

    override suspend fun getEmployerRatings(employerId: String): Flow<ApiResult<List<EmployerRatingRepository.Rating>>> = flow {
        emit(ApiResult.Loading)
        try {
            // Get from local cache first
            val cachedRatings = withContext(ioDispatcher) {
                ratingDao.getRatingsByEmployerId(employerId)
            }

            if (cachedRatings.isNotEmpty()) {
                val domainRatings = cachedRatings.map {
                    EmployerRatingRepository.Rating(
                        rating = it.rating,
                        review = it.review.takeIf { review -> review.isNotBlank() },
                        userId = it.userId,
                        timestamp = it.timestamp
                    )
                }

                logger.d(
                    tag = TAG,
                    message = "Returning cached employer ratings",
                    additionalData = mapOf(
                        "employer_id" to employerId,
                        "count" to domainRatings.size
                    )
                )

                emit(ApiResult.Success(domainRatings))
            }

            // Fetch from remote
            withContext(ioDispatcher) {
                try {
                    val ratings = supabaseClient.client.postgrest["employer_ratings"]
                        .select {
                            filter { eq("employer_id", employerId)}
                            order("timestamp", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        }
                        .decodeList<Map<String, Any?>>()

                    val domainRatings = ratings.map {
                        EmployerRatingRepository.Rating(
                            rating = (it["rating"] as? Number)?.toFloat() ?: 0.0f,
                            review = (it["review"] as? String)?.takeIf { review -> review.isNotBlank() },
                            userId = it["user_id"] as String,
                            timestamp = (it["timestamp"] as? Number)?.toLong() ?: 0L
                        )
                    }

                    // Update local cache
                    ratings.forEach {
                        val entity = RatingEntity(
                            id = it["id"] as String,
                            userId = it["user_id"] as String,
                            employerId = employerId,
                            rating = (it["rating"] as? Number)?.toFloat() ?: 0.0f,
                            review = (it["review"] as? String) ?: "",
                            timestamp = (it["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                        ratingDao.insertOrUpdateRating(entity)
                    }

                    logger.i(
                        tag = TAG,
                        message = "Successfully fetched employer ratings from network",
                        additionalData = mapOf(
                            "employer_id" to employerId,
                            "count" to domainRatings.size
                        )
                    )

                    // Only emit if different from cache
                    if (cachedRatings.size != ratings.size) {
                        emit(ApiResult.Success(domainRatings))
                    }
                } catch (e: Exception) {
                    // If remote fetch fails but we have cached data, don't emit error
                    if (cachedRatings.isEmpty()) {
                        logger.w(
                            tag = TAG,
                            message = "Error fetching employer ratings from network",
                            throwable = e,
                            additionalData = mapOf("employer_id" to employerId)
                        )
                        emit(ApiResult.Success(emptyList()))
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting employer ratings",
                throwable = e,
                additionalData = mapOf("employer_id" to employerId)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_EMPLOYER_RATINGS")))
        }
    }

    private fun isRatingCacheExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > CACHE_DURATION
    }

    private fun getCurrentUserId(): String {
        return encryptedPreferences.getUserId()
    }
}