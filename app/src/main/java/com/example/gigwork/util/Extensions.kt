package com.example.gigwork.util

import com.example.gigwork.core.error.extensions.toAppError
import com.example.gigwork.core.result.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

fun <T> Flow<T>.asResult(): Flow<ApiResult<T>> {
    return this
        .map<T, ApiResult<T>> { ApiResult.Success(it) }
        .onStart { emit(ApiResult.Loading) }
        .catch { emit(ApiResult.Error(it.toAppError())) }
}