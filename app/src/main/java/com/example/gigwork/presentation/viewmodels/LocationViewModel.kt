package com.example.gigwork.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.example.gigwork.core.error.handler.GlobalErrorHandler
import com.example.gigwork.core.error.model.*
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.repository.LocationRepository
import com.example.gigwork.domain.usecase.location.GetStatesAndDistrictsUseCase
import com.example.gigwork.presentation.states.FavoriteLocation
import com.example.gigwork.presentation.states.RecentLocation
import com.example.gigwork.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.delay
import javax.inject.Inject
import java.util.UUID
import kotlin.math.pow

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
    object RequestLocationPermission : LocationEvent()
    object OpenLocationSettings : LocationEvent()
    object ClearSelection : LocationEvent()
}

enum class LocationTab {
    LIST, MAP, FAVORITES
}

enum class LocationType {
    HOME, WORK, OTHER
}

data class LocationState(
    val states: List<String> = emptyList(),
    val districts: List<String> = emptyList(),
    val searchResults: List<Location> = emptyList(),
    val recentLocations: List<RecentLocation> = emptyList(),
    val favoriteLocations: List<FavoriteLocation> = emptyList(),
    val selectedState: String? = null,
    val selectedDistrict: String? = null,
    val selectedTab: LocationTab = LocationTab.LIST,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val mapZoomLevel: Float = 12f,
    val formattedAddress: String? = null,
    val pinCode: String? = null,
    val isLoadingStates: Boolean = false,
    val isLoadingDistricts: Boolean = false,
    val isResolvingLocation: Boolean = false,
    val isSearching: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val isLocationEnabled: Boolean = false,
    val errorMessage: ErrorMessage? = null,
    val locationError: LocationError? = null,
    val lastUpdated: Long = 0L,
    val isCacheValid: Boolean = false
)

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getStatesAndDistrictsUseCase: GetStatesAndDistrictsUseCase,
    private val locationRepository: LocationRepository,
    private val favoriteLocationRepository: FavoriteLocationRepository,
    private val errorHandler: GlobalErrorHandler,
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    companion object {
        private const val TAG = "LocationViewModel"
        private const val KEY_SELECTED_STATE = "selectedState"
        private const val KEY_SELECTED_DISTRICT = "selectedDistrict"
        private const val KEY_ZOOM_LEVEL = "zoomLevel"
        private const val KEY_SELECTED_TAB = "selectedTab"
        private const val LOCATION_CACHE_DURATION = 24 * 60 * 60 * 1000L // 24 hours
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY = 1000L
    }

    private val _state = MutableStateFlow(LocationState())
    val state = _state.asStateFlow()

    private val _events = MutableStateFlow<LocationEvent?>(null)
    val events = _events.asStateFlow()

    init {
        initializeData()
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }

    private fun initializeData() {
        viewModelScope.launch {
            loadStates()
            loadRecentLocations()
            loadFavoriteLocations()
            restoreSelectedLocation()
            checkLocationPermissions()
        }
    }

    private fun restoreSelectedLocation() {
        val savedState = savedStateHandle.get<String>(KEY_SELECTED_STATE)
        val savedDistrict = savedStateHandle.get<String>(KEY_SELECTED_DISTRICT)
        val savedZoomLevel = savedStateHandle.get<Float>(KEY_ZOOM_LEVEL)
        val savedTab = savedStateHandle.get<LocationTab>(KEY_SELECTED_TAB)

        updateState {
            copy(
                selectedState = savedState,
                selectedDistrict = savedDistrict,
                mapZoomLevel = savedZoomLevel ?: 12f,
                selectedTab = savedTab ?: LocationTab.LIST
            )
        }

        savedState?.let { loadDistricts(it) }
    }

    fun loadStates() {
        viewModelScope.launch {
            updateState { copy(isLoadingStates = true) }

            getStatesAndDistrictsUseCase.getStates()
                .retryWithExponentialBackoff()
                .catch { e -> handleError(e.toAppError()) }
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            updateState {
                                copy(
                                    states = result.data.sorted(),
                                    isLoadingStates = false,
                                    lastUpdated = System.currentTimeMillis(),
                                    isCacheValid = true
                                )
                            }
                        }
                        is Result.Error -> handleError(result.error)
                        is Result.Loading -> updateState { copy(isLoadingStates = true) }
                    }
                }
        }
    }

    fun updateSelectedState(state: String) {
        viewModelScope.launch {
            savedStateHandle[KEY_SELECTED_STATE] = state
            savedStateHandle[KEY_SELECTED_DISTRICT] = null

            updateState {
                copy(
                    selectedState = state,
                    selectedDistrict = null,
                    districts = emptyList()
                )
            }

            loadDistricts(state)
        }
    }

    private fun loadDistricts(state: String) {
        if (!validateState(state)) return

        viewModelScope.launch {
            updateState { copy(isLoadingDistricts = true) }

            getStatesAndDistrictsUseCase.getDistricts(state)
                .retryWithExponentialBackoff()
                .catch { e -> handleError(e.toAppError()) }
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            updateState {
                                copy(
                                    districts = result.data.sorted(),
                                    isLoadingDistricts = false
                                )
                            }
                        }
                        is Result.Error -> handleError(result.error)
                        is Result.Loading -> updateState { copy(isLoadingDistricts = true) }
                    }
                }
        }
    }

    fun updateSelectedDistrict(district: String) {
        viewModelScope.launch {
            savedStateHandle[KEY_SELECTED_DISTRICT] = district

            val currentState = state.value.selectedState
            if (currentState != null) {
                updateState { copy(selectedDistrict = district) }
                addToRecentLocations(currentState, district)
                emitEvent(LocationEvent.LocationSelected(currentState, district))
            } else {
                handleError(AppError.ValidationError("No state selected"))
            }
        }
    }

    fun searchLocations(query: String) {
        if (!validateSearchQuery(query)) return

        viewModelScope.launch {
            updateState { copy(isSearching = true) }

            try {
                withContext(ioDispatcher) {
                    locationRepository.searchLocations(query)
                        .retryWithExponentialBackoff()
                        .catch { e -> handleError(e.toAppError()) }
                        .collect { result ->
                            when (result) {
                                is Result.Success -> {
                                    updateState {
                                        copy(
                                            searchResults = result.data,
                                            isSearching = false
                                        )
                                    }
                                    emitEvent(LocationEvent.LocationSearched(query, result.data))
                                }
                                is Result.Error -> handleError(result.error)
                                is Result.Loading -> updateState { copy(isSearching = true) }
                            }
                        }
                }
            } catch (e: Exception) {
                handleError(e.toAppError())
            }
        }
    }

    fun updateMapLocation(latitude: Double, longitude: Double, zoomLevel: Float) {
        if (!validateCoordinates(latitude, longitude)) return

        viewModelScope.launch {
            savedStateHandle[KEY_ZOOM_LEVEL] = zoomLevel
            updateState {
                copy(
                    latitude = latitude,
                    longitude = longitude,
                    mapZoomLevel = zoomLevel
                )
            }
            resolveLocation(latitude, longitude)
        }
    }

    private suspend fun resolveLocation(latitude: Double, longitude: Double) {
        updateState { copy(isResolvingLocation = true) }

        try {
            withContext(ioDispatcher) {
                locationRepository.getLocationFromCoordinates(latitude, longitude)
                    .retryWithExponentialBackoff()
                    .catch { e -> handleError(e.toAppError()) }
                    .collect { result ->
                        when (result) {
                            is Result.Success -> {
                                val location = result.data
                                updateState {
                                    copy(
                                        selectedState = location.state,
                                        selectedDistrict = location.district,
                                        formattedAddress = location.fullAddress,
                                        pinCode = location.pinCode,
                                        isResolvingLocation = false
                                    )
                                }
                                emitEvent(
                                    LocationEvent.LocationResolved(
                                        state = location.state,
                                        district = location.district,
                                        fullAddress = location.fullAddress,
                                        pinCode = location.pinCode
                                    )
                                )
                            }
                            is Result.Error -> {
                                updateState {
                                    copy(
                                        locationError = LocationError.ResolutionFailed(result.error.message),
                                        isResolvingLocation = false
                                    )
                                }
                            }
                            is Result.Loading -> updateState { copy(isResolvingLocation = true) }
                        }
                    }
            }
        } catch (e: Exception) {
            handleError(e.toAppError())
            updateState { copy(isResolvingLocation = false) }
        }
    }

    private fun addToRecentLocations(state: String, district: String) {
        viewModelScope.launch {
            val recentLocation = RecentLocation(
                state = state,
                district = district,
                timestamp = System.currentTimeMillis(),
                latitude = this@LocationViewModel.state.value.latitude,
                longitude = this@LocationViewModel.state.value.longitude
            )
            locationRepository.addRecentLocation(recentLocation)
            loadRecentLocations()
        }
    }

    private fun loadRecentLocations() {
        viewModelScope.launch {
            locationRepository.getRecentLocations()
                .retryWithExponentialBackoff()
                .catch { e -> handleError(e.toAppError()) }
                .collect { result ->
                    when (result) {
                        is Result.Success -> updateState { copy(recentLocations = result.data) }
                        is Result.Error -> handleError(result.error)
                        is Result.Loading -> Unit // Don't show loading for recent locations
                    }
                }
        }
    }

    fun addToFavorites(name: String, type: LocationType) {
        viewModelScope.launch {
            val currentState = state.value
            if (!validateCurrentLocation(currentState)) return@launch

            val favoriteLocation = FavoriteLocation(
                id = UUID.randomUUID().toString(),
                name = name,
                state = currentState.selectedState!!,
                district = currentState.selectedDistrict!!,
                latitude = currentState.latitude,
                longitude = currentState.longitude,
                address = currentState.formattedAddress,
                type = type
            )

            favoriteLocationRepository.addFavoriteLocation(favoriteLocation)
            loadFavoriteLocations()
            emitEvent(LocationEvent.LocationSaved(favoriteLocation))
        }
    }

    private fun loadFavoriteLocations() {
        viewModelScope.launch {
            favoriteLocationRepository.getFavoriteLocations()
                .retryWithExponentialBackoff()
                .catch { e -> handleError(e.toAppError()) }
                .collect { result ->
                    when (result) {
                        is Result.Success -> updateState { copy(favoriteLocations = result.data) }
                        is Result.Error -> handleError(result.error)
                        is Result.Loading -> Unit // Don't show loading for favorites
                    }
                }
        }
    }

    fun updateTab(tab: LocationTab) {
        savedStateHandle[KEY_SELECTED_TAB] = tab
        updateState { copy(selectedTab = tab) }
    }

    fun checkLocationPermissions() {
        viewModelScope.launch {
            val hasPermission = locationRepository.hasLocationPermission()
            val isEnabled = locationRepository.isLocationEnabled()

            updateState {
                copy(
                    hasLocationPermission = hasPermission,
                    isLocationEnabled = isEnabled
                )
            }

            when {
                !hasPermission -> emitEvent(LocationEvent.RequestLocationPermission)
                !isEnabled -> emitEvent(LocationEvent.OpenLocationSettings)
            }
        }
    }

    private fun handleError(error: AppError) {
        logger.e(
            tag = TAG,
            message = "Error in LocationViewModel: ${error.message}",
            throwable = error,
            additionalData = mapOf(
                "selectedState" to state.value.selectedState,
                "selectedDistrict" to state.value.selectedDistrict,
                "operation" to getCurrentOperation(),
                "timestamp" to System.currentTimeMillis()
            )
        )

        updateState {
            copy(
                isLoadingStates = false,
                isLoadingDistricts = false,
                isResolvingLocation = false,
                isSearching = false,
                errorMessage = error.toErrorMessage()
            )
        }
    }

    private fun getCurrentOperation(): String = with(state.value) {
        return when {
            isLoadingStates -> "loading_states"
            isLoadingDistricts -> "loading_districts"
            isResolvingLocation -> "resolving_location"
            isSearching -> "searching"
            else -> "unknown"
        }
    }

    fun retry() {
        viewModelScope.launch {
            clearError()
            state.value.selectedState?.let {
                loadDistricts(it)
            } ?: loadStates()
        }
    }

    fun clearError() {
        updateState {
            copy(
                errorMessage = null,
                locationError = null
            )
        }
    }

    fun clearSelection() {
        viewModelScope.launch {
            savedStateHandle[KEY_SELECTED_STATE] = null
            savedStateHandle[KEY_SELECTED_DISTRICT] = null

            updateState {
                copy(
                    selectedState = null,
                    selectedDistrict = null,
                    districts = emptyList(),
                    latitude = null,
                    longitude = null,
                    formattedAddress = null,
                    pinCode = null
                )
            }
            emitEvent(LocationEvent.ClearSelection)
        }
    }

    private fun validateState(state: String): Boolean {
        return state.isNotEmpty() && !state.value.isLoadingStates
    }

    private fun validateSearchQuery(query: String): Boolean {
        return query.length >= 2
    }

    private fun validateCoordinates(latitude: Double, longitude: Double): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    private fun validateCurrentLocation(currentState: LocationState): Boolean {
        return currentState.selectedState != null &&
                currentState.selectedDistrict != null
    }

    private fun isCacheValid(): Boolean {
        val lastUpdated = state.value.lastUpdated
        return System.currentTimeMillis() - lastUpdated < LOCATION_CACHE_DURATION
    }

    private fun emitEvent(event: LocationEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    private fun updateState(update: LocationState.() -> LocationState) {
        _state.update { it.update() }
    }

    private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.retryWithExponentialBackoff(): kotlinx.coroutines.flow.Flow<T> {
        return this.retryWhen { cause, attempt ->
            if (attempt < MAX_RETRY_ATTEMPTS) {
                delay(INITIAL_RETRY_DELAY * 2.0.pow(attempt.toDouble()).toLong())
                true
            } else {
                false
            }
        }
    }
}