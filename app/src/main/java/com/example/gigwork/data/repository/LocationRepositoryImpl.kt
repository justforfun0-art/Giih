package com.example.gigwork.data.repository

import com.example.gigwork.core.error.ExceptionMapper
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.data.api.LocationService
import com.example.gigwork.domain.models.Location
import com.example.gigwork.domain.repository.LocationRepository
import com.example.gigwork.presentation.states.RecentLocation
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationService: LocationService
) : LocationRepository {

    // Store recent locations in memory (alternatively, this could be saved in a database)
    private val recentLocations = mutableListOf<RecentLocation>()

    override suspend fun getStates(): Flow<ApiResult<List<String>>> = flow {
        emit(ApiResult.Loading)
        try {
            locationService.getStates()
                .catch { e ->
                    emit(ApiResult.Error(ExceptionMapper.map(e, "GET_STATES")))
                }
                .collect { states ->
                    emit(ApiResult.Success(states.sorted()))
                }
        } catch (e: Exception) {
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_STATES")))
        }
    }

    override suspend fun getDistricts(state: String): Flow<ApiResult<List<String>>> = flow {
        emit(ApiResult.Loading)
        try {
            locationService.getDistricts(state)
                .catch { e ->
                    emit(ApiResult.Error(ExceptionMapper.map(e, "GET_DISTRICTS")))
                }
                .collect { districts ->
                    emit(ApiResult.Success(districts.sorted()))
                }
        } catch (e: Exception) {
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_DISTRICTS")))
        }
    }

    override suspend fun getLocationFromCoordinates(latitude: Double, longitude: Double): Flow<ApiResult<Location>> = flow {
        emit(ApiResult.Loading)
        try {
            locationService.getLocationFromCoordinates(latitude, longitude)
                .catch { e ->
                    emit(ApiResult.Error(ExceptionMapper.map(e, "GET_LOCATION_FROM_COORDINATES")))
                }
                .collect { location ->
                    emit(ApiResult.Success(location))
                }
        } catch (e: Exception) {
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_LOCATION_FROM_COORDINATES")))
        }
    }

    override suspend fun searchLocationsByQuery(query: String): Flow<ApiResult<List<Location>>> = flow {
        emit(ApiResult.Loading)

        try {
            // In a full implementation, this would query a geocoding API with the search term
            // For now, providing a placeholder implementation

            // Simulate a delay for the search operation
            kotlinx.coroutines.delay(500)

            // If query is empty, return empty results
            if (query.isBlank()) {
                emit(ApiResult.Success(emptyList()))
                return@flow
            }

            // Create a mock result for demonstration purposes
            val results = listOf(
                Location(
                    latitude = null,
                    longitude = null,
                    address = "Sample address matching $query",
                    pinCode = null,
                    state = "Sample State",
                    district = "Sample District"
                )
            )

            emit(ApiResult.Success(results))
        } catch (e: Exception) {
            emit(ApiResult.Error(ExceptionMapper.map(e, "SEARCH_LOCATIONS")))
        }
    }

    override suspend fun getRecentLocations(): Flow<ApiResult<List<RecentLocation>>> = flow {
        emit(ApiResult.Loading)

        try {
            // Return stored recent locations
            emit(ApiResult.Success(recentLocations.toList()))
        } catch (e: Exception) {
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_RECENT_LOCATIONS")))
        }
    }

    override suspend fun addRecentLocation(location: RecentLocation) {
        try {
            // Remove if this location already exists to avoid duplicates
            // Use state + district as a unique identifier instead of id
            recentLocations.removeIf {
                it.state == location.state && it.district == location.district
            }

            // Add to the beginning of the list
            recentLocations.add(0, location)

            // Keep only the 10 most recent locations
            if (recentLocations.size > 10) {
                recentLocations.removeAt(recentLocations.size - 1)
            }
        } catch (e: Exception) {
            // Log error but don't throw - this is a non-critical operation
            println("Error adding recent location: ${e.message}")
        }
    }

    override suspend fun hasLocationPermission(): Boolean {
        // In a real implementation, this would check for location permissions
        // This requires access to Android's context and permission APIs
        return true
    }

    override suspend fun isLocationEnabled(): Boolean {
        // In a real implementation, this would check if device location is enabled
        // This requires access to Android's location services
        return true
    }

    suspend fun clearLocationCache() {
        locationService.clearCache()
    }
}