// presentation/events/LocationEvent.kt
package com.example.gigwork.presentation.events

import com.example.gigwork.domain.models.Location
import com.example.gigwork.presentation.states.FavoriteLocation

sealed class LocationEvent {
    data class LocationSelected(val state: String, val district: String) : LocationEvent()
    data class LocationSearched(val query: String, val results: List<Location>) : LocationEvent()
    data class LocationResolved(
        val state: String,
        val district: String,
        val fullAddress: String,
        val pinCode: String
    ) : LocationEvent()
    data class LocationSaved(val location: FavoriteLocation) : LocationEvent()
    data class CoordinatesUpdated(
        val latitude: Double,
        val longitude: Double,
        val address: String? = null
    ) : LocationEvent()
    data class ValidationError(
        val message: String,
        val field: String? = null
    ) : LocationEvent()
    object RequestLocationPermission : LocationEvent()
    object OpenLocationSettings : LocationEvent()
    object ClearSelection : LocationEvent()
}