package com.example.gigwork.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.example.gigwork.core.error.handler.GlobalErrorHandler
import com.example.gigwork.core.error.model.*
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.Location
import com.example.gigwork.domain.repository.FavoriteLocationRepository
import com.example.gigwork.domain.repository.LocationRepository
import com.example.gigwork.domain.repository.LocationResult
import com.example.gigwork.domain.usecase.location.GetStatesAndDistrictsUseCase
import com.example.gigwork.presentation.states.FavoriteLocation
import com.example.gigwork.presentation.states.LocationError
import com.example.gigwork.presentation.states.RecentLocation
import com.example.gigwork.presentation.states.LocationState
import com.example.gigwork.presentation.states.LocationTab
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
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import java.util.UUID
import kotlin.math.pow
import com.example.gigwork.presentation.events.LocationEvent
import com.example.gigwork.presentation.states.LocationSearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow
import com.example.gigwork.presentation.states.LocationType


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
        private const val LOCATION_CACHE_DURATION = 24 * 60 * 60 * 1000L
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY = 1000L
        private const val MIN_SEARCH_QUERY_LENGTH = 2
    }

    private val _state = MutableStateFlow(LocationState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<LocationEvent>()
    val events = _events.asSharedFlow()

    init {
        initializeData()
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
            try {
                updateState { copy(isLoadingStates = true) }

                locationRepository.getStates()
                    .retryWhen { cause, attempt ->
                        if (attempt < MAX_RETRY_ATTEMPTS) {
                            delay(INITIAL_RETRY_DELAY * 2.0.pow(attempt.toDouble()).toLong())
                            true
                        } else {
                            false
                        }
                    }
                    .catch { e ->
                        val error = when (e) {
                            is AppError -> e
                            else -> AppError.UnexpectedError(
                                message = e.message ?: "Failed to load states",
                                cause = e
                            )
                        }
                        handleError(error)
                    }
                    .collect { apiResult ->
                        when (apiResult) {
                            is ApiResult.Success<List<String>> -> {
                                updateState {
                                    copy(
                                        states = apiResult.data.sorted(),
                                        isLoadingStates = false,
                                        lastUpdated = System.currentTimeMillis(),
                                        isCacheValid = true
                                    )
                                }
                            }
                            is ApiResult.Error -> handleError(apiResult.error)
                            is ApiResult.Loading -> updateState { copy(isLoadingStates = true) }
                        }
                    }
            } catch (e: Exception) {
                val error = when (e) {
                    is AppError -> e
                    else -> AppError.UnexpectedError(
                        message = e.message ?: "Failed to load states",
                        cause = e
                    )
                }
                handleError(error)
            } finally {
                updateState { copy(isLoadingStates = false) }
            }
        }
    }
    fun loadDistricts(state: String) {
        if (!validateState(state)) return

        viewModelScope.launch {
            try {
                updateState { copy(isLoadingDistricts = true) }

                locationRepository.getDistricts(state)
                    .retryWhen { cause, attempt ->
                        if (attempt < MAX_RETRY_ATTEMPTS) {
                            delay(INITIAL_RETRY_DELAY * 2.0.pow(attempt.toDouble()).toLong())
                            true
                        } else {
                            false
                        }
                    }
                    .catch { e ->
                        val error = when (e) {
                            is AppError -> e
                            else -> AppError.UnexpectedError(
                                message = e.message ?: "Failed to load districts",
                                cause = e
                            )
                        }
                        handleError(error)
                    }
                    .collect { apiResult ->
                        when (apiResult) {
                            is ApiResult.Success<List<String>> -> {
                                updateState {
                                    copy(
                                        districts = apiResult.data.sorted(),
                                        isLoadingDistricts = false
                                    )
                                }
                            }
                            is ApiResult.Error -> handleError(apiResult.error)
                            is ApiResult.Loading -> updateState { copy(isLoadingDistricts = true) }
                        }
                    }
            } catch (e: Exception) {
                val error = when (e) {
                    is AppError -> e
                    else -> AppError.UnexpectedError(
                        message = e.message ?: "Failed to load districts",
                        cause = e
                    )
                }
                handleError(error)
            } finally {
                updateState { copy(isLoadingDistricts = false) }
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

    fun updateSelectedDistrict(district: String) {
        viewModelScope.launch {
            val currentState = state.value.selectedState
            if (currentState != null) {
                savedStateHandle[KEY_SELECTED_DISTRICT] = district
                updateState { copy(selectedDistrict = district) }
                addToRecentLocations(currentState, district)
                emitLocationEvent(LocationEvent.LocationSelected(currentState, district))
            } else {
                handleError(AppError.ValidationError("No state selected"))
            }
        }
    }

    // First, let's add a mapper function
    private fun Location.toLocationSearchResult(): LocationSearchResult {
        return LocationSearchResult(
            state = this.state,
            district = this.district,
            fullAddress = this.address ?: "",
            distance = null,
            latitude = this.latitude,
            longitude = this.longitude,
            type = LocationType.OTHER
        )
    }
    fun searchLocations(query: String) {
        if (!validateSearchQuery(query)) return

        viewModelScope.launch {
            try {
                updateState { copy(isSearching = true) }

                withContext(ioDispatcher) {
                    locationRepository.searchLocationsByQuery(query)
                        .retryWhen { cause, attempt ->
                            if (attempt < MAX_RETRY_ATTEMPTS) {
                                delay(INITIAL_RETRY_DELAY * 2.0.pow(attempt.toDouble()).toLong())
                                true
                            } else {
                                false
                            }
                        }
                        .catch { e ->
                            val error = when (e) {
                                is AppError -> e
                                else -> AppError.UnexpectedError(
                                    message = e.message ?: "Failed to search locations",
                                    cause = e
                                )
                            }
                            handleError(error)
                        }
                        .collect { apiResult ->
                            when (apiResult) {
                                is ApiResult.Success<List<Location>> -> {
                                    val searchResults = apiResult.data.map { it.toLocationSearchResult() }
                                    updateState {
                                        copy(
                                            searchResults = searchResults,
                                            isSearching = false
                                        )
                                    }
                                    emitLocationEvent(LocationEvent.LocationSearched(
                                        query = query,
                                        results = apiResult.data
                                    ))
                                }
                                is ApiResult.Error -> handleError(apiResult.error)
                                is ApiResult.Loading -> updateState { copy(isSearching = true) }
                            }
                        }
                }
            } catch (e: Exception) {
                val error = when (e) {
                    is AppError -> e
                    else -> AppError.UnexpectedError(
                        message = e.message ?: "Failed to search locations",
                        cause = e
                    )
                }
                handleError(error)
            } finally {
                updateState { copy(isSearching = false) }
            }
        }
    }

    private fun validateSearchRequest(query: String): Boolean {
        if (query.length < MIN_SEARCH_QUERY_LENGTH) {
            handleError(AppError.ValidationError(
                message = "Search query must be at least $MIN_SEARCH_QUERY_LENGTH characters",
                field = "query"
            ))
            return false
        }
        return true
    }

    private fun validateLocationData(location: Location): Boolean {
        return when {
            location.state.isBlank() -> {
                handleError(AppError.ValidationError(
                    message = "State cannot be empty",
                    field = "state"
                ))
                false
            }
            location.district.isBlank() -> {
                handleError(AppError.ValidationError(
                    message = "District cannot be empty",
                    field = "district"
                ))
                false
            }
            else -> true
        }
    }

    // State Management Functions
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

    // Additional Validation Functions
    private fun validateLocationPermissions(): Boolean {
        val currentState = state.value
        if (!currentState.hasLocationPermission) {
            emitLocationEvent(LocationEvent.RequestLocationPermission)
            return false
        }
        if (!currentState.isLocationEnabled) {
            emitLocationEvent(LocationEvent.OpenLocationSettings)
            return false
        }
        return true
    }

    private fun validateMapState(): Boolean {
        val currentState = state.value
        return when {
            !currentState.isMapVisible -> {
                handleError(AppError.ValidationError(
                    message = "Map is not currently visible",
                    field = "map_visibility"
                ))
                false
            }
            currentState.mapZoomLevel < 0 -> {
                handleError(AppError.ValidationError(
                    message = "Invalid map zoom level",
                    field = "zoom_level"
                ))
                false
            }
            else -> true
        }
    }

    // Cache Management Functions
    private fun validateCache(): Boolean {
        return when {
            !state.value.isCacheValid -> {
                refreshLocationData()
                false
            }
            System.currentTimeMillis() - state.value.lastUpdated > LOCATION_CACHE_DURATION -> {
                refreshLocationData()
                false
            }
            else -> true
        }
    }

    private fun refreshLocationData() {
        viewModelScope.launch {
            loadStates()
            state.value.selectedState?.let { loadDistricts(it) }
        }
    }

    // Location Data Management
    private fun updateLocationData(location: Location) {
        updateState {
            copy(
                selectedState = location.state,
                selectedDistrict = location.district,
                latitude = location.latitude,
                longitude = location.longitude,
                formattedAddress = location.address,
                lastUpdated = System.currentTimeMillis(),
                isCacheValid = true
            )
        }
    }

    // Event Handling
    private fun handleLocationEvent(event: LocationEvent) {
        viewModelScope.launch {
            when (event) {
                is LocationEvent.LocationSelected -> {
                    addToRecentLocations(event.state, event.district)
                }
                is LocationEvent.CoordinatesUpdated -> {
                    updateMapLocation(event.latitude, event.longitude, state.value.mapZoomLevel)
                }
                is LocationEvent.ValidationError -> {
                    handleError(AppError.ValidationError(
                        message = event.message,
                        field = event.field
                    ))
                }
                else -> Unit
            }
        }
    }

    // Utility Functions
    private fun getLocationIdentifier(state: String, district: String): String {
        return "$state:$district"
    }

    private fun isValidLocationPair(state: String?, district: String?): Boolean {
        return !state.isNullOrBlank() && !district.isNullOrBlank()
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
                    .retryWhen { cause, attempt ->
                        if (attempt < MAX_RETRY_ATTEMPTS) {
                            delay(INITIAL_RETRY_DELAY * 2.0.pow(attempt.toDouble()).toLong())
                            true
                        } else {
                            false
                        }
                    }
                    .catch { e ->
                        val error = when (e) {
                            is AppError -> e
                            else -> AppError.UnexpectedError(
                                message = e.message ?: "Failed to resolve location",
                                cause = e
                            )
                        }
                        handleError(error)
                    }
                    .collect { apiResult ->
                        when (apiResult) {
                            is ApiResult.Success<Location> -> {
                                val location = apiResult.data
                                updateState {
                                    copy(
                                        selectedState = location.state,
                                        selectedDistrict = location.district,
                                        formattedAddress = location.address,
                                        pinCode = location.pinCode,
                                        isResolvingLocation = false
                                    )
                                }
                                emitLocationEvent(
                                    LocationEvent.LocationResolved(
                                        state = location.state,
                                        district = location.district,
                                        fullAddress = location.address ?: "",
                                        pinCode = location.pinCode ?: ""
                                    )
                                )
                            }
                            is ApiResult.Error -> {
                                updateState {
                                    copy(
                                        locationError = LocationError.ResolutionFailed(apiResult.error.message),
                                        isResolvingLocation = false
                                    )
                                }
                            }
                            is ApiResult.Loading -> updateState { copy(isResolvingLocation = true) }
                        }
                    }
            }
        } catch (e: Exception) {
            val error = when (e) {
                is AppError -> e
                else -> AppError.UnexpectedError(
                    message = e.message ?: "Failed to resolve location",
                    cause = e
                )
            }
            handleError(error)
        } finally {
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
            try {
                locationRepository.getRecentLocations()
                    .retryWhen { cause, attempt ->
                        if (attempt < MAX_RETRY_ATTEMPTS) {
                            delay(INITIAL_RETRY_DELAY * 2.0.pow(attempt.toDouble()).toLong())
                            true
                        } else {
                            false
                        }
                    }
                    .catch { e ->
                        val error = when (e) {
                            is AppError -> e
                            else -> AppError.UnexpectedError(
                                message = e.message ?: "Failed to load recent locations",
                                cause = e
                            )
                        }
                        handleError(error)
                    }
                    .collect { apiResult ->
                        when (apiResult) {
                            is ApiResult.Success<List<RecentLocation>> -> {
                                updateState { copy(recentLocations = apiResult.data) }
                            }
                            is ApiResult.Error -> handleError(apiResult.error)
                            is ApiResult.Loading -> Unit
                        }
                    }
            } catch (e: Exception) {
                val error = when (e) {
                    is AppError -> e
                    else -> AppError.UnexpectedError(
                        message = e.message ?: "Failed to load recent locations",
                        cause = e
                    )
                }
                handleError(error)
            }
        }
    }
    fun addToFavorites(name: String, type: com.example.gigwork.presentation.states.LocationType) {
        viewModelScope.launch {
            try {
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
                    .collect { result ->
                        when (result) {
                            is ApiResult.Success<Unit> -> {
                                loadFavoriteLocations()
                                emitLocationEvent(LocationEvent.LocationSaved(favoriteLocation))
                            }
                            is ApiResult.Error -> handleError(result.error)
                            is ApiResult.Loading -> Unit
                        }
                    }
            } catch (e: Exception) {
                val error = when (e) {
                    is AppError -> e
                    else -> AppError.UnexpectedError(
                        message = e.message ?: "Failed to add favorite location",
                        cause = e
                    )
                }
                handleError(error)
            }
        }
    }

    private fun loadFavoriteLocations() {
        viewModelScope.launch {
            try {
                favoriteLocationRepository.getFavoriteLocations()
                    .retryWhen { cause, attempt ->
                        if (attempt < MAX_RETRY_ATTEMPTS) {
                            delay(INITIAL_RETRY_DELAY * 2.0.pow(attempt.toDouble()).toLong())
                            true
                        } else {
                            false
                        }
                    }
                    .catch { e ->
                        val error = when (e) {
                            is AppError -> e
                            else -> AppError.UnexpectedError(
                                message = e.message ?: "Failed to load favorite locations",
                                cause = e
                            )
                        }
                        handleError(error)
                    }
                    .collect { apiResult ->
                        when (apiResult) {
                            is ApiResult.Success<List<FavoriteLocation>> -> {
                                updateState { copy(favoriteLocations = apiResult.data) }
                            }
                            is ApiResult.Error -> handleError(apiResult.error)
                            is ApiResult.Loading -> Unit
                        }
                    }
            } catch (e: Exception) {
                val error = when (e) {
                    is AppError -> e
                    else -> AppError.UnexpectedError(
                        message = e.message ?: "Failed to load favorite locations",
                        cause = e
                    )
                }
                handleError(error)
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
                !hasPermission -> emitLocationEvent(LocationEvent.RequestLocationPermission)
                !isEnabled -> emitLocationEvent(LocationEvent.OpenLocationSettings)
            }
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
            emitLocationEvent(LocationEvent.ClearSelection)
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

        val errorMessage = errorHandler.handleCoreError(error)
        updateState {
            copy(
                isLoadingStates = false,
                isLoadingDistricts = false,
                isResolvingLocation = false,
                isSearching = false,
                errorMessage = errorMessage
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

    private fun validateState(state: String): Boolean {
        return state.isNotEmpty() && !this.state.value.isLoadingStates
    }

    private fun validateSearchQuery(query: String): Boolean {
        return query.length >= MIN_SEARCH_QUERY_LENGTH
    }

    private fun validateCoordinates(latitude: Double, longitude: Double): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    private fun validateCurrentLocation(currentState: LocationState): Boolean {
        return currentState.selectedState != null && currentState.selectedDistrict != null
    }

    private fun isCacheValid(): Boolean {
        val lastUpdated = state.value.lastUpdated
        return System.currentTimeMillis() - lastUpdated < LOCATION_CACHE_DURATION
    }

    private fun emitLocationEvent(event: LocationEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    private fun updateState(update: LocationState.() -> LocationState) {
        _state.update { it.update() }
    }

    private suspend fun <T> Flow<T>.retryWithExponentialBackoff(): Flow<T> {
        return this.retryWhen { cause, attempt ->
            if (attempt < MAX_RETRY_ATTEMPTS) {
                delay(INITIAL_RETRY_DELAY * 2.0.pow(attempt.toDouble()).toLong())
                true
            } else {
                false
            }
        }
    }

    private fun Exception.toAppError(): AppError {
        return when (this) {
            is AppError -> this
            else -> AppError.UnexpectedError(
                message = this.message ?: "An unexpected error occurred",
                cause = this
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }
}