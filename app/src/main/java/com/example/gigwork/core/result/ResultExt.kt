package com.example.gigwork.core.result

import com.example.gigwork.core.error.model.AppError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

fun <T> Flow<T>.asResult(): Flow<ApiResult<T>> {
    return this
        .map<T, ApiResult<T>> { ApiResult.success(it) }
        .onStart { emit(ApiResult.loading()) }
        .catch { emit(ApiResult.error(it.toAppError())) }
}



suspend fun <T> ApiResult<T>.suspendOnSuccess(
    action: suspend (T) -> Unit
): ApiResult<T> {
    if (this is ApiResult.Success) {
        action(data)
    }
    return this
}

suspend fun <T> ApiResult<T>.suspendOnError(
    action: suspend (AppError) -> Unit
): ApiResult<T> {
    if (this is ApiResult.Error) {
        action(error)
    }
    return this
}



fun Throwable.toAppError(): AppError {
    return when (this) {
        is AppError -> this
        is retrofit2.HttpException -> AppError.NetworkError(
            message = message ?: "Network error occurred",
            cause = this,
            httpCode = code()
        )
        is java.net.SocketTimeoutException -> AppError.NetworkError(
            message = "Connection timed out",
            cause = this,
            isConnectionError = true
        )
        is java.io.IOException -> AppError.NetworkError(
            message = "Network error occurred",
            cause = this,
            isConnectionError = true
        )
        else -> AppError.UnexpectedError(
            message = message ?: "An unexpected error occurred",
            cause = this
        )
    }
}