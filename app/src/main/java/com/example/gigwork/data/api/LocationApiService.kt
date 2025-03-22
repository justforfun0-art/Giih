package com.example.gigwork.data.api

import com.example.gigwork.data.cache.LocationCache
import com.example.gigwork.data.models.DistrictsResponse
import com.example.gigwork.data.models.StatesResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import javax.inject.Inject
import javax.inject.Singleton

interface LocationApiService {
    @GET("postoffice/states")
    suspend fun getStatesRaw(): Response<StatesResponse>

    @GET("postoffice/districts/{state}")
    suspend fun getDistrictsRaw(@Path("state") state: String): Response<DistrictsResponse>
}

@Singleton
class LocationService @Inject constructor(
    private val api: LocationApiService,
    private val cache: LocationCache,
    private val rateLimiter: RateLimiter
) {
    companion object {
        private const val STATES_CACHE_KEY = "states"
        private const val DISTRICT_CACHE_KEY = "districts_"
    }

    suspend fun getStates(): Flow<List<String>> = flow {
        try {
            // Check cache first
            cache.get(STATES_CACHE_KEY)?.let {
                emit(it)
                return@flow
            }

            // Check rate limit
            if (!rateLimiter.checkRateLimit(STATES_CACHE_KEY)) {
                throw LocationApiException.RateLimitException()
            }

            // Make API call
            val response = api.getStatesRaw()
            if (response.isSuccessful) {
                response.body()?.let { statesResponse ->
                    // Extract just the state names
                    val stateNames = statesResponse.states.map { it.name }
                    // Cache the result
                    cache.put(STATES_CACHE_KEY, stateNames)
                    emit(stateNames)
                } ?: throw LocationApiException.NoDataException()
            } else {
                when (response.code()) {
                    429 -> throw LocationApiException.RateLimitException()
                    503 -> throw LocationApiException.ServiceUnavailableException()
                    else -> throw LocationApiException.NetworkException(
                        "Failed to fetch states: ${response.message()}"
                    )
                }
            }
        } catch (e: LocationApiException) {
            throw e
        } catch (e: Exception) {
            throw LocationApiException.NetworkException(
                "Error fetching states: ${e.message}"
            )
        }
    }

    suspend fun getDistricts(state: String): Flow<List<String>> = flow {
        try {
            val cacheKey = "$DISTRICT_CACHE_KEY$state"
            // Check cache first
            cache.get(cacheKey)?.let {
                emit(it)
                return@flow
            }

            // Check rate limit
            if (!rateLimiter.checkRateLimit(cacheKey)) {
                throw LocationApiException.RateLimitException()
            }

            // Make API call
            val response = api.getDistrictsRaw(state)
            if (response.isSuccessful) {
                response.body()?.let { districtsResponse ->
                    // Extract just the state names
                    val districtnames = districtsResponse.districts.map { it.name }
                    // Cache the result
                    cache.put(cacheKey, districtnames)
                    emit(districtnames)
                } ?: throw LocationApiException.NoDataException()
            } else {
                when (response.code()) {
                    404 -> throw LocationApiException.InvalidStateException(state)
                    429 -> throw LocationApiException.RateLimitException()
                    503 -> throw LocationApiException.ServiceUnavailableException()
                    else -> throw LocationApiException.NetworkException(
                        "Failed to fetch districts: ${response.message()}"
                    )
                }
            }
        } catch (e: LocationApiException) {
            throw e
        } catch (e: Exception) {
            throw LocationApiException.NetworkException(
                "Error fetching districts: ${e.message}"
            )
        }
    }
    // Add this method to LocationService class
    suspend fun getLocationFromCoordinates(latitude: Double, longitude: Double): Flow<com.example.gigwork.domain.models.Location> = flow {
        // In a real implementation, this would call a geocoding API
        // For now, creating a placeholder implementation
        val location = com.example.gigwork.domain.models.Location(
            latitude = latitude,
            longitude = longitude,
            address = null,
            pinCode = null,
            state = "Unknown", // Default values when geocoding isn't available
            district = "Unknown"
        )
        emit(location)
    }
    suspend fun clearCache() {
        cache.clear()
    }
}