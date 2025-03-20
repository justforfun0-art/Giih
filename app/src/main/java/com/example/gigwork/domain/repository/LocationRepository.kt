package com.example.gigwork.domain.repository

import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.Location
import com.example.gigwork.presentation.states.RecentLocation
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    suspend fun getStates(): Flow<ApiResult<List<String>>>
    suspend fun getDistricts(state: String): Flow<ApiResult<List<String>>>
    suspend fun getLocationFromCoordinates(
        latitude: Double,
        longitude: Double
    ): Flow<ApiResult<Location>>
    suspend fun searchLocationsByQuery(query: String): Flow<ApiResult<List<Location>>>
    suspend fun getRecentLocations(): Flow<ApiResult<List<RecentLocation>>>
    suspend fun addRecentLocation(location: RecentLocation)
    suspend fun hasLocationPermission(): Boolean
    suspend fun isLocationEnabled(): Boolean
}

data class LocationResult(
    val state: String,
    val district: String,
    val fullAddress: String,
    val pinCode: String
)