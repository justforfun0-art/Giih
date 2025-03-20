package com.example.gigwork.domain.repository

import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.Location
import kotlinx.coroutines.flow.Flow

interface UserLocationRepository {
    /**
     * Get current user location
     * @return Flow of ApiResult containing the current location
     */
    suspend fun getUserLocation(): Flow<ApiResult<Location>>

    /**
     * Update user's current location
     * @param location New location information
     * @return Flow of ApiResult indicating success or failure
     */
    suspend fun updateUserLocation(location: Location): Flow<ApiResult<Boolean>>

    /**
     * Get last known location for the user
     * @return Flow of ApiResult containing the last known location
     */
    suspend fun getLastKnownLocation(): Flow<ApiResult<Location>>

    /**
     * Observe user location updates
     * @return Flow of ApiResult containing location updates
     */
    fun observeUserLocation(): Flow<ApiResult<Location>>

    /**
     * Get list of recent locations
     * @param limit Maximum number of locations to retrieve
     * @return Flow of ApiResult containing list of recent locations
     */
    suspend fun getRecentLocations(limit: Int = 10): Flow<ApiResult<List<Location>>>
}