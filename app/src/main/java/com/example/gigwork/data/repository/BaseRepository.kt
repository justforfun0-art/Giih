// data/repository/BaseRepository.kt
package com.example.gigwork.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response

abstract class BaseRepository {
    protected suspend fun <T> safeApiCall(
        apiCall: suspend () -> Response<T>
    ): Flow<T> = flow {
        try {
            val response = apiCall()
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(it)
                } ?: throw Exception("Response body is null")
            } else {
                throw Exception("Error ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            throw e
        }
    }
}