package com.example.gigwork.domain.usecase.location

import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStatesAndDistrictsUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    suspend fun getStates(): Flow<ApiResult<List<String>>> {
        return locationRepository.getStates()
    }

    suspend fun getDistricts(state: String): Flow<ApiResult<List<String>>> {
        require(state.isNotBlank()) { "State cannot be empty" }
        return locationRepository.getDistricts(state)
    }
}