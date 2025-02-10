package com.example.gigwork.domain.repository

import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    suspend fun getStates(): Flow<List<String>>
    suspend fun getDistricts(state: String): Flow<List<String>>
}
