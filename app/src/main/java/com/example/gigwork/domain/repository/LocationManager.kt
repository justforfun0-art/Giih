package com.example.gigwork.domain.repository

import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.Location
import kotlinx.coroutines.flow.Flow

interface LocationManager {
    /**
     * Calculate distance between two points
     * @return Distance in kilometers
     */
    fun calculateDistance(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Double

    /**
     * Check if location permission is granted
     * @return True if permission is granted
     */
    fun isLocationPermissionGranted(): Boolean

    /**
     * Request location permission from user
     */
    fun requestLocationPermission()

    /**
     * Get current location updates
     * @return Flow of ApiResult containing location updates
     */
    fun getCurrentLocation(): Flow<ApiResult<LocationUpdate>>

    /**
     * Start receiving location updates
     */
    fun startLocationUpdates()

    /**
     * Stop receiving location updates
     */
    fun stopLocationUpdates()

    /**
     * Get last known location
     * @return Flow of ApiResult containing the last known location
     */
    suspend fun getLastKnownLocation(): Flow<ApiResult<Location>>

    data class LocationUpdate(
        val location: Location,
        val accuracy: Float,
        val timestamp: Long,
        val provider: String
    )
}