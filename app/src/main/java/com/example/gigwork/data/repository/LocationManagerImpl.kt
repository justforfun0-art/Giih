package com.example.gigwork.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.example.gigwork.core.error.ExceptionMapper
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.Location
import com.example.gigwork.domain.repository.LocationManager
import com.example.gigwork.util.Logger
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class LocationManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) : LocationManager {

    companion object {
        private const val TAG = "LocationManager"
        private const val LOCATION_REQUEST_INTERVAL = 10000L // 10 seconds
        private const val LOCATION_FASTEST_INTERVAL = 5000L // 5 seconds
        private const val EARTH_RADIUS = 6371.0 // kilometers
    }

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private var locationCallback: LocationCallback? = null
    private var permissionRequestLauncher: ActivityResultLauncher<String>? = null

    override fun calculateDistance(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Double {
        // Haversine formula to calculate distance between two points on Earth
        val latDistance = Math.toRadians(endLat - startLat)
        val lngDistance = Math.toRadians(endLng - startLng)

        val a = sin(latDistance / 2).pow(2) +
                cos(Math.toRadians(startLat)) *
                cos(Math.toRadians(endLat)) *
                sin(lngDistance / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS * c
    }

    override fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    override fun requestLocationPermission() {
        // This implementation is limited as it requires an Activity context
        // In a real implementation, this would use ActivityResultLauncher
        // For this repository implementation, we'll log this limitation

        logger.w(
            tag = TAG,
            message = "requestLocationPermission called, but implementation requires ActivityResultLauncher",
            additionalData = mapOf(
                "canRequestPermission" to "false",
                "reason" to "Repository implementation cannot request permissions directly"
            )
        )

        // The actual implementation would be:
        // permissionRequestLauncher?.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun getCurrentLocation(): Flow<ApiResult<LocationManager.LocationUpdate>> = callbackFlow {
        if (!isLocationEnabled()) {
            trySend(ApiResult.Error(ExceptionMapper.map(
                IllegalStateException("Location services disabled"),
                "GET_CURRENT_LOCATION_DISABLED"
            )))
            close()
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_REQUEST_INTERVAL)
            .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { androidLocation ->
                    val location = Location(
                        latitude = androidLocation.latitude,
                        longitude = androidLocation.longitude,
                        state = "",  // These will be populated via reverse geocoding
                        district = "",
                        address = null,
                        pinCode = null
                    )

                    val locationUpdate = LocationManager.LocationUpdate(
                        location = location,
                        accuracy = androidLocation.accuracy,
                        timestamp = androidLocation.time,
                        provider = androidLocation.provider ?: "unknown"
                    )

                    trySend(ApiResult.Success(locationUpdate))

                    // Optional: Enhance with reverse geocoding
                    // This would be done asynchronously in a real implementation
                }
            }
        }

        try {
            trySend(ApiResult.Loading)

            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )

                logger.d(
                    tag = TAG,
                    message = "Started location updates for getCurrentLocation"
                )
            } else {
                trySend(ApiResult.Error(ExceptionMapper.map(
                    SecurityException("Location permission not granted"),
                    "GET_CURRENT_LOCATION_PERMISSION"
                )))
                close()
                return@callbackFlow
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error requesting location updates",
                throwable = e
            )
            trySend(ApiResult.Error(ExceptionMapper.map(e, "GET_CURRENT_LOCATION")))
            close(e)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
            logger.d(
                tag = TAG,
                message = "Removed location updates for getCurrentLocation"
            )
        }
    }
    @SuppressLint("MissingPermission")
    override fun startLocationUpdates() {
        if (!isLocationPermissionGranted()) {
            logger.w(
                tag = TAG,
                message = "Cannot start location updates: permission not granted"
            )
            return
        }

        if (!isLocationEnabled()) {
            logger.w(
                tag = TAG,
                message = "Cannot start location updates: location services disabled"
            )
            return
        }

        // Stop any existing updates
        stopLocationUpdates()

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_REQUEST_INTERVAL)
            .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { androidLocation ->
                    logger.d(
                        tag = TAG,
                        message = "Received location update",
                        additionalData = mapOf(
                            "latitude" to androidLocation.latitude,
                            "longitude" to androidLocation.longitude,
                            "accuracy" to androidLocation.accuracy,
                            "provider" to (androidLocation.provider ?: "unknown")
                        )
                    )
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )

            logger.i(
                tag = TAG,
                message = "Started location updates"
            )
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error starting location updates",
                throwable = e
            )
        }
    }

    override fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
            logger.i(
                tag = TAG,
                message = "Stopped location updates"
            )
        }
    }

    override suspend fun getLastKnownLocation(): Flow<ApiResult<Location>> = flow {
        emit(ApiResult.Loading)

        if (!isLocationPermissionGranted()) {
            emit(ApiResult.Error(ExceptionMapper.map(
                SecurityException("Location permission not granted"),
                "GET_LAST_KNOWN_LOCATION_PERMISSION"
            )))
            return@flow
        }

        try {
            @SuppressLint("MissingPermission")
            val lastLocation = suspendCancellableCoroutine<android.location.Location?> { continuation ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        continuation.resume(location)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }

            if (lastLocation != null) {
                val location = Location(
                    latitude = lastLocation.latitude,
                    longitude = lastLocation.longitude,
                    state = "",  // Would be populated via reverse geocoding
                    district = "",
                    address = null,
                    pinCode = null
                )

                logger.d(
                    tag = TAG,
                    message = "Retrieved last known location",
                    additionalData = mapOf(
                        "latitude" to lastLocation.latitude,
                        "longitude" to lastLocation.longitude,
                        "accuracy" to lastLocation.accuracy,
                        "provider" to (lastLocation.provider ?: "unknown")
                    )
                )

                // Optional: Enhance with reverse geocoding
                try {
                    val geocodedLocation = reverseGeocode(lastLocation.latitude, lastLocation.longitude)
                    emit(ApiResult.Success(geocodedLocation))
                } catch (e: Exception) {
                    // If geocoding fails, still return the basic location
                    logger.w(
                        tag = TAG,
                        message = "Geocoding failed, returning basic location",
                        throwable = e
                    )
                    emit(ApiResult.Success(location))
                }
            } else {
                logger.w(
                    tag = TAG,
                    message = "Last known location is null"
                )
                emit(ApiResult.Error(ExceptionMapper.map(
                    NoSuchElementException("Last known location not available"),
                    "GET_LAST_KNOWN_LOCATION_NULL"
                )))
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting last known location",
                throwable = e
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_LAST_KNOWN_LOCATION")))
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }

    @Suppress("DEPRECATION") // Using Geocoder with listeners requires API 33+
    private suspend fun reverseGeocode(latitude: Double, longitude: Double): Location {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses.isNullOrEmpty()) {
                return Location(
                    latitude = latitude,
                    longitude = longitude,
                    state = "",
                    district = "",
                    address = null,
                    pinCode = null
                )
            }

            val address = addresses[0]

            Location(
                latitude = latitude,
                longitude = longitude,
                state = address.adminArea ?: "",
                district = address.subAdminArea ?: "",
                address = address.getAddressLine(0),
                pinCode = address.postalCode
            )
        } catch (e: Exception) {
            logger.w(
                tag = TAG,
                message = "Geocoding failed",
                throwable = e
            )

            // Return basic location without geocoded info
            Location(
                latitude = latitude,
                longitude = longitude,
                state = "",
                district = "",
                address = null,
                pinCode = null
            )
        }
    }
}