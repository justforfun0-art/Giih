// domain/repository/FavoriteLocationRepository.kt
package com.example.gigwork.domain.repository

import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.presentation.states.FavoriteLocation
import kotlinx.coroutines.flow.Flow

interface FavoriteLocationRepository {
    suspend fun addFavoriteLocation(location: FavoriteLocation): Flow<ApiResult<Unit>>
    suspend fun getFavoriteLocations(): Flow<ApiResult<List<FavoriteLocation>>>
}