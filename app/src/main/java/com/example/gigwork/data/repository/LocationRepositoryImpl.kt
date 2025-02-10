package com.example.gigwork.data.repository

import com.example.gigwork.data.api.LocationService
import com.example.gigwork.domain.repository.LocationRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationService: LocationService
) : LocationRepository {

    override suspend fun getStates(): Flow<Result<List<String>>> = flow {
        emit(Result.Loading)
        try {
            locationService.getStates()
                .catch { e ->
                    emit(Result.Error(Exception(e.message)))
                }
                .collect { states ->
                    emit(Result.Success(states.sorted()))
                }
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override suspend fun getDistricts(state: String): Flow<Result<List<String>>> = flow {
        emit(Result.Loading)
        try {
            locationService.getDistricts(state)
                .catch { e ->
                    emit(Result.Error(Exception(e.message)))
                }
                .collect { districts ->
                    emit(Result.Success(districts.sorted()))
                }
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    suspend fun clearLocationCache() {
        locationService.clearCache()
    }
}