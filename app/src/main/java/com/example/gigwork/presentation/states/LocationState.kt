package com.example.gigwork.presentation.states

import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.presentation.base.UiState

data class LocationState(
    // Data States
    val states: List<String> = emptyList(),
    val districts: List<String> = emptyList(),
    val selectedState: String? = null,
    val selectedDistrict: String? = null,
    val recentLocations: List<RecentLocation> = emptyList(),
    val favoriteLocations: List<FavoriteLocation> = emptyList(),

    // Loading States
    val isLoadingStates: Boolean = false,
    val isLoadingDistricts: Boolean = false,
    val isLoading: Boolean = false,
    val isResolvingLocation: Boolean = false,

    // Error States
    val errorMessage: ErrorMessage? = null,
    val validationError: String? = null,
    val locationError: LocationError? = null,

    // Cache States
    val lastUpdated: Long = 0,
    val isCacheValid: Boolean = false,
    val cacheExpiryTime: Long = 24 * 60 * 60 * 1000, // 24 hours in milliseconds

    // Location Data
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val formattedAddress: String? = null,
    val pinCode: String? = null,
    val landmark: String? = null,

    // Search States
    val searchQuery: String = "",
    val searchResults: List<LocationSearchResult> = emptyList(),
    val isSearching: Boolean = false,

    // Permissions
    val hasLocationPermission: Boolean = false,
    val isLocationEnabled: Boolean = false,

    // UI States
    val isMapVisible: Boolean = false,
    val mapZoomLevel: Float = 12f,
    val selectedTab: LocationTab = LocationTab.LIST
) : UiState<LocationState> {

    // Helper functions
    fun isLocationSelected(): Boolean =
        selectedState != null && selectedDistrict != null

    fun getFormattedLocation(): String = when {
        selectedDistrict != null && selectedState != null ->
            "$selectedDistrict, $selectedState"
        selectedState != null -> selectedState
        else -> ""
    }

    fun hasValidCoordinates(): Boolean =
        latitude != null && longitude != null

    fun isValidLocation(): Boolean =
        isLocationSelected() && hasValidCoordinates()

    fun getLocationError(): String? = when (locationError) {
        is LocationError.PermissionDenied -> "Location permission required"
        is LocationError.LocationDisabled -> "Please enable location services"
        is LocationError.ResolutionFailed -> "Failed to resolve location"
        null -> null
    }
}

sealed class LocationEvent {
    data class LocationSelected(
        val state: String,
        val district: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : LocationEvent()

    data class CoordinatesUpdated(
        val latitude: Double,
        val longitude: Double,
        val address: String? = null,
        val accuracy: Float? = null
    ) : LocationEvent()

    data class ValidationError(
        val message: String,
        val field: String? = null
    ) : LocationEvent()

    data class LocationResolved(
        val state: String,
        val district: String,
        val fullAddress: String,
        val pinCode: String? = null,
        val landmark: String? = null
    ) : LocationEvent()

    data class LocationSaved(
        val location: FavoriteLocation
    ) : LocationEvent()

    data class LocationSearched(
        val query: String,
        val results: List<LocationSearchResult>
    ) : LocationEvent()

    data class PermissionResult(
        val granted: Boolean,
        val shouldShowRationale: Boolean
    ) : LocationEvent()

    data class MapMoved(
        val newLatitude: Double,
        val newLongitude: Double,
        val zoomLevel: Float
    ) : LocationEvent()

    object ClearSelection : LocationEvent()
    object LoadingStarted : LocationEvent()
    object LoadingCompleted : LocationEvent()
    object RequestLocationPermission : LocationEvent()
    object OpenLocationSettings : LocationEvent()
}

// Supporting data classes
data class RecentLocation(
    val state: String,
    val district: String,
    val timestamp: Long,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class FavoriteLocation(
    val id: String,
    val name: String,
    val state: String,
    val district: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val type: LocationType = LocationType.OTHER
)

data class LocationSearchResult(
    val state: String,
    val district: String,
    val fullAddress: String,
    val distance: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val type: LocationType = LocationType.OTHER
)

sealed class LocationError {
    object PermissionDenied : LocationError()
    object LocationDisabled : LocationError()
    data class ResolutionFailed(val reason: String) : LocationError()
}

enum class LocationType {
    HOME,
    WORK,
    OTHER
}

enum class LocationTab {
    LIST,
    MAP,
    RECENT,
    FAVORITE
}