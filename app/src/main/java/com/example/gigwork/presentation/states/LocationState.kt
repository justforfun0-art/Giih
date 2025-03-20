package com.example.gigwork.presentation.states

import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.presentation.base.UiState
import com.example.gigwork.presentation.events.LocationEvent

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
    override val isLoading: Boolean = false,
    val isResolvingLocation: Boolean = false,

    // Error States
    override val errorMessage: ErrorMessage? = null,
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

    override fun copy(isLoading: Boolean, errorMessage: ErrorMessage?): LocationState {
        return copy(
            isLoading = isLoading,
            errorMessage = errorMessage
        )
    }

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