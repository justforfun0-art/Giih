package com.example.gigwork.data.repository

import android.content.Context
import com.example.gigwork.core.error.ExceptionMapper
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.data.security.EncryptedPreferences
import com.example.gigwork.data.database.LocationDao
import com.example.gigwork.data.database.LocationEntity
import com.example.gigwork.domain.models.Location
import com.example.gigwork.domain.repository.UserLocationRepository
import com.example.gigwork.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserLocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient,
    private val locationDao: LocationDao,
    private val encryptedPreferences: EncryptedPreferences,
    private val logger: Logger
) : UserLocationRepository {

    companion object {
        private const val TAG = "UserLocationRepository"
        private const val LOCATION_CACHE_DURATION = 5 * 60 * 1000L // 5 minutes
        private const val LOCATION_UPDATE_INTERVAL = 60 * 1000L // 1 minute
    }

    private val _locationUpdates = MutableSharedFlow<ApiResult<Location>>(replay = 1)
    private var isTracking = false
    private val locationScope = CoroutineScope(Dispatchers.IO)

    override suspend fun getUserLocation(): Flow<ApiResult<Location>> = flow {
        emit(ApiResult.Loading)
        try {
            val userId = getCurrentUserId()
            if (userId.isBlank()) {
                emit(ApiResult.Error(ExceptionMapper.map(IllegalStateException("User not logged in"), "GET_USER_LOCATION")))
                return@flow
            }

            // Try to get from local cache
            val cachedLocation = locationDao.getUserLastLocation(userId)
            if (cachedLocation != null && !isLocationCacheExpired(cachedLocation.timestamp)) {
                emit(ApiResult.Success(mapEntityToDomain(cachedLocation)))
            }

            // Fetch from remote
            withContext(Dispatchers.IO) {
                try {
                    val userLocation = supabaseClient.client.postgrest["user_locations"]
                        .select {
                            filter { eq("user_id", userId)}
                            order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                            limit(1)
                        }
                        .decodeSingleOrNull<Map<String, Any?>>()

                    if (userLocation != null) {
                        val location = mapResponseToDomain(userLocation)
                        saveLocationToCache(userId, location)
                        emit(ApiResult.Success(location))
                    } else if (cachedLocation == null) {
                        // If no remote or cached location available
                        emit(ApiResult.Error(ExceptionMapper.map(NoSuchElementException("No location found"), "GET_USER_LOCATION")))
                    }
                } catch (e: Exception) {
                    // If remote fetch fails but we have cached data, don't emit error
                    if (cachedLocation == null) {
                        emit(ApiResult.Error(ExceptionMapper.map(e, "GET_USER_LOCATION_NETWORK")))
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error getting user location", e)
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_USER_LOCATION")))
        }
    }

    override suspend fun updateUserLocation(location: Location): Flow<ApiResult<Boolean>> = flow {
        emit(ApiResult.Loading)
        try {
            val userId = getCurrentUserId()
            if (userId.isBlank()) {
                emit(ApiResult.Error(ExceptionMapper.map(IllegalStateException("User not logged in"), "UPDATE_USER_LOCATION")))
                return@flow
            }

            // Update location in remote database
            withContext(Dispatchers.IO) {
                val payload = mapOf(
                    "user_id" to userId,
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "state" to location.state,
                    "district" to location.district,
                    "created_at" to System.currentTimeMillis()
                )

                supabaseClient.client.postgrest["user_locations"]
                    .insert(payload)
            }

            // Update in local cache
            saveLocationToCache(userId, location)

            emit(ApiResult.Success(true))
            logger.d(TAG, "Location updated successfully", mapOf(
                "userId" to userId,
                "latitude" to location.latitude,
                "longitude" to location.longitude
            ))
        } catch (e: Exception) {
            logger.e(TAG, "Error updating user location", e)
            emit(ApiResult.Error(ExceptionMapper.map(e, "UPDATE_USER_LOCATION")))
        }
    }

    override suspend fun getLastKnownLocation(): Flow<ApiResult<Location>> = flow {
        emit(ApiResult.Loading)
        try {
            val userId = getCurrentUserId()
            if (userId.isBlank()) {
                emit(ApiResult.Error(ExceptionMapper.map(IllegalStateException("User not logged in"), "GET_LAST_KNOWN_LOCATION")))
                return@flow
            }

            // Get from local cache first
            val cachedLocation = locationDao.getUserLastLocation(userId)
            if (cachedLocation != null) {
                emit(ApiResult.Success(mapEntityToDomain(cachedLocation)))
            } else {
                // If not in cache, try to get from remote
                withContext(Dispatchers.IO) {
                    try {
                        val response = supabaseClient.client.postgrest["user_locations"]
                            .select {
                                filter { eq("user_id", userId)}
                                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                                limit(1)
                            }
                            .decodeSingleOrNull<Map<String, Any?>>()

                        if (response != null) {
                            val location = mapResponseToDomain(response)
                            saveLocationToCache(userId, location)
                            emit(ApiResult.Success(location))
                        } else {
                            emit(ApiResult.Error(ExceptionMapper.map(NoSuchElementException("No location found"), "GET_LAST_KNOWN_LOCATION")))
                        }
                    } catch (e: Exception) {
                        emit(ApiResult.Error(ExceptionMapper.map(e, "GET_LAST_KNOWN_LOCATION_NETWORK")))
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error getting last known location", e)
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_LAST_KNOWN_LOCATION")))
        }
    }

    override fun observeUserLocation(): Flow<ApiResult<Location>> = _locationUpdates

    override suspend fun getRecentLocations(limit: Int): Flow<ApiResult<List<Location>>> = flow {
        emit(ApiResult.Loading)
        try {
            val userId = getCurrentUserId()
            if (userId.isBlank()) {
                emit(ApiResult.Error(ExceptionMapper.map(IllegalStateException("User not logged in"), "GET_RECENT_LOCATIONS")))
                return@flow
            }

            // Get from local cache first
            val cachedLocations = locationDao.getUserLocations(userId, limit)
            if (cachedLocations.isNotEmpty()) {
                emit(ApiResult.Success(cachedLocations.map { mapEntityToDomain(it) }))
            }

            // Get from remote
            withContext(Dispatchers.IO) {
                try {
                    val response = supabaseClient.client.postgrest["user_locations"]
                        .select {
                            filter { eq("user_id", userId)}
                            order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                            limit(limit.toLong())
                        }
                        .decodeList<Map<String, Any?>>()

                    if (response.isNotEmpty()) {
                        val locations = response.map { mapResponseToDomain(it) }

                        // Update cache with new data
                        locations.forEach { saveLocationToCache(userId, it) }

                        emit(ApiResult.Success(locations))
                    } else if (cachedLocations.isEmpty()) {
                        // Only emit empty list if we don't have cache
                        emit(ApiResult.Success(emptyList()))
                    }
                } catch (e: Exception) {
                    // If remote fetch fails but we have cached data, don't emit error
                    if (cachedLocations.isEmpty()) {
                        emit(ApiResult.Error(ExceptionMapper.map(e, "GET_RECENT_LOCATIONS_NETWORK")))
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error getting recent locations", e)
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_RECENT_LOCATIONS")))
        }
    }

    private fun startLocationTracking() {
        if (isTracking) return

        isTracking = true
        locationScope.launch {
            while (isTracking) {
                try {
                    val userId = getCurrentUserId()
                    if (userId.isNotBlank()) {
                        // Get latest location from remote
                        val location = supabaseClient.client.postgrest["user_locations"]
                            .select {
                                filter { eq("user_id", userId)}
                                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                                limit(1)
                            }
                            .decodeSingleOrNull<Map<String, Any?>>()

                        if (location != null) {
                            val domainLocation = mapResponseToDomain(location)
                            saveLocationToCache(userId, domainLocation)
                            _locationUpdates.emit(ApiResult.Success(domainLocation))
                        }
                    }
                } catch (e: Exception) {
                    logger.e(TAG, "Error in location tracking", e)
                    _locationUpdates.emit(ApiResult.Error(ExceptionMapper.map(e, "TRACK_LOCATION")))
                }
                delay(LOCATION_UPDATE_INTERVAL)
            }
        }
    }

    private fun stopLocationTracking() {
        isTracking = false
    }

    private fun isLocationCacheExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > LOCATION_CACHE_DURATION
    }

    private suspend fun saveLocationToCache(userId: String, location: Location) {
        val entity = LocationEntity(
            id = 0, // Auto-generated
            userId = userId,
            latitude = location.latitude ?: 0.0,
            longitude = location.longitude ?: 0.0,
            state = location.state,
            district = location.district,
            address = location.address ?: "",
            pinCode = location.pinCode ?: "",
            timestamp = System.currentTimeMillis()
        )
        locationDao.insertLocation(entity)
    }

    private fun mapEntityToDomain(entity: LocationEntity): Location {
        return Location(
            latitude = entity.latitude,
            longitude = entity.longitude,
            state = entity.state,
            district = entity.district,
            address = entity.address.takeIf { it.isNotBlank() },
            pinCode = entity.pinCode.takeIf { it.isNotBlank() }
        )
    }

    private fun mapResponseToDomain(response: Map<String, Any?>): Location {
        return Location(
            latitude = (response["latitude"] as? Number)?.toDouble(),
            longitude = (response["longitude"] as? Number)?.toDouble(),
            state = (response["state"] as? String) ?: "",
            district = (response["district"] as? String) ?: "",
            address = response["address"] as? String,
            pinCode = response["pin_code"] as? String
        )
    }

    private fun getCurrentUserId(): String {
        return encryptedPreferences.getUserId()
    }
}