package com.example.gigwork.data.repository

import com.example.gigwork.core.error.ExceptionMapper
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.data.database.FavoriteLocationDao
import com.example.gigwork.data.database.FavoriteLocationEntity
import com.example.gigwork.data.security.EncryptedPreferences
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.repository.FavoriteLocationRepository
import com.example.gigwork.presentation.states.FavoriteLocation
import com.example.gigwork.util.Logger
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteLocationRepositoryImpl @Inject constructor(
    private val favoriteLocationDao: FavoriteLocationDao,
    private val supabaseClient: SupabaseClient,
    private val encryptedPreferences: EncryptedPreferences,
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FavoriteLocationRepository {

    companion object {
        private const val TAG = "FavLocationRepo"
    }

    override suspend fun addFavoriteLocation(location: FavoriteLocation): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        try {
            val userId = getCurrentUserId()
            if (userId.isBlank()) {
                emit(ApiResult.Error(ExceptionMapper.map(IllegalStateException("User not logged in"), "ADD_FAVORITE_LOCATION")))
                return@flow
            }

            // Check if location already exists as favorite
            val existingLocation = withContext(ioDispatcher) {
                favoriteLocationDao.getFavoriteLocationByStateAndDistrict(
                    userId = userId,
                    state = location.state,
                    district = location.district
                )
            }

            if (existingLocation != null) {
                logger.d(
                    tag = TAG,
                    message = "Location already exists as favorite, updating",
                    additionalData = mapOf(
                        "state" to location.state,
                        "district" to location.district
                    )
                )
                // Delete existing to ensure clean update
                favoriteLocationDao.deleteFavoriteLocation(existingLocation.id)
            }

            // Insert into local database
            val entity = FavoriteLocationEntity(
                id = 0, // Auto-generated
                userId = userId,
                name = location.name,
                state = location.state,
                district = location.district,
                latitude = location.latitude ?: 0.0,
                longitude = location.longitude ?: 0.0,
                timestamp = System.currentTimeMillis()
            )

            val localId = favoriteLocationDao.insertFavoriteLocation(entity)

            // Insert into remote database
            withContext(ioDispatcher) {
                try {
                    val payload = mapOf(
                        "user_id" to userId,
                        "name" to location.name,
                        "state" to location.state,
                        "district" to location.district,
                        "latitude" to (location.latitude ?: 0.0),
                        "longitude" to (location.longitude ?: 0.0),
                        "created_at" to System.currentTimeMillis()
                    )

                    supabaseClient.client.postgrest["favorite_locations"]
                        .insert(payload)

                    logger.i(
                        tag = TAG,
                        message = "Successfully saved favorite location to remote",
                        additionalData = mapOf(
                            "state" to location.state,
                            "district" to location.district
                        )
                    )
                } catch (e: Exception) {
                    // Log error but don't fail the operation since local save succeeded
                    logger.w(
                        tag = TAG,
                        message = "Failed to save favorite location to remote, will sync later",
                        throwable = e,
                        additionalData = mapOf(
                            "state" to location.state,
                            "district" to location.district
                        )
                    )
                    // We can implement a sync mechanism later if needed
                }
            }

            logger.i(
                tag = TAG,
                message = "Successfully added favorite location",
                additionalData = mapOf(
                    "state" to location.state,
                    "district" to location.district,
                    "local_id" to localId
                )
            )

            // Clean up any potential duplicates
            withContext(ioDispatcher) {
                favoriteLocationDao.cleanupDuplicateLocations(userId)
            }

            emit(ApiResult.Success(Unit))
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error adding favorite location",
                throwable = e,
                additionalData = mapOf(
                    "state" to location.state,
                    "district" to location.district
                )
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "ADD_FAVORITE_LOCATION")))
        }
    }

    override suspend fun getFavoriteLocations(): Flow<ApiResult<List<FavoriteLocation>>> = flow {
        emit(ApiResult.Loading)
        try {
            val userId = getCurrentUserId()
            if (userId.isBlank()) {
                emit(ApiResult.Error(ExceptionMapper.map(IllegalStateException("User not logged in"), "GET_FAVORITE_LOCATIONS")))
                return@flow
            }

            // First get from local database
            val localLocations = withContext(ioDispatcher) {
                favoriteLocationDao.getUserFavoriteLocations(userId)
            }

            if (localLocations.isNotEmpty()) {
                val domainLocations = localLocations.map { mapEntityToDomain(it) }

                logger.d(
                    tag = TAG,
                    message = "Returning cached favorite locations",
                    additionalData = mapOf(
                        "count" to domainLocations.size
                    )
                )

                emit(ApiResult.Success(domainLocations))
            }

            // Then try to refresh from remote
            withContext(ioDispatcher) {
                try {
                    val remoteLocations = supabaseClient.client.postgrest["favorite_locations"]
                        .select {
                            filter { eq("user_id", userId)}
                        }
                        .decodeList<Map<String, Any?>>()

                    if (remoteLocations.isNotEmpty()) {
                        // Convert to entities and update local database
                        val entities = remoteLocations.map { mapResponseToEntity(it, userId) }

                        // Clear existing locations and insert fresh ones
                        favoriteLocationDao.clearUserFavoriteLocations(userId)
                        entities.forEach { favoriteLocationDao.insertFavoriteLocation(it) }

                        // Get fresh locations from database (now ordered correctly)
                        val updatedLocalLocations = favoriteLocationDao.getUserFavoriteLocations(userId)
                        val updatedDomainLocations = updatedLocalLocations.map { mapEntityToDomain(it) }

                        logger.i(
                            tag = TAG,
                            message = "Successfully refreshed favorite locations from remote",
                            additionalData = mapOf(
                                "count" to updatedDomainLocations.size
                            )
                        )

                        // Only emit if different from local
                        if (localLocations.size != updatedLocalLocations.size) {
                            emit(ApiResult.Success(updatedDomainLocations))
                        }
                    } else if (localLocations.isEmpty()) {
                        // If no locations found locally or remotely
                        emit(ApiResult.Success(emptyList()))
                    }
                } catch (e: Exception) {
                    // If remote fetch fails but we have cached data, don't emit error
                    if (localLocations.isEmpty()) {
                        logger.w(
                            tag = TAG,
                            message = "Error fetching favorite locations from remote",
                            throwable = e
                        )
                        emit(ApiResult.Success(emptyList()))
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting favorite locations",
                throwable = e
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_FAVORITE_LOCATIONS")))
        }
    }

    /**
     * Get the current user ID from encrypted preferences
     */
    private fun getCurrentUserId(): String {
        return encryptedPreferences.getUserId()
    }

    /**
     * Map a FavoriteLocationEntity to a FavoriteLocation domain model
     */
    private fun mapEntityToDomain(entity: FavoriteLocationEntity): FavoriteLocation {
        return FavoriteLocation(
            id = entity.id.toString(),
            name = entity.name,
            state = entity.state,
            district = entity.district,
            latitude = entity.latitude,
            longitude = entity.longitude
        )
    }

    /**
     * Map a Supabase response to a FavoriteLocationEntity
     */
    private fun mapResponseToEntity(
        response: Map<String, Any?>,
        userId: String
    ): FavoriteLocationEntity {
        return FavoriteLocationEntity(
            id = 0, // Auto-generated
            userId = userId,
            name = response["name"] as? String ?: "",
            state = response["state"] as? String ?: "",
            district = response["district"] as? String ?: "",
            latitude = (response["latitude"] as? Number)?.toDouble() ?: 0.0,
            longitude = (response["longitude"] as? Number)?.toDouble() ?: 0.0,
            timestamp = (response["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}