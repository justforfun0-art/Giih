// core/result/ResultExt.kt
package com.example.gigwork.core.result

import com.example.gigwork.core.error.model.AppError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

fun <T> Flow<T>.asResult(): Flow<Result<T>> {
    return this
        .map<T, Result<T>> { Result.success(it) }
        .onStart { emit(Result.loading()) }
        .catch { emit(Result.error(it.toAppError())) }
}

suspend fun <T> Result<T>.suspendOnSuccess(
    action: suspend (T) -> Unit
): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

suspend fun <T> Result<T>.suspendOnError(
    action: suspend (AppError) -> Unit
): Result<T> {
    if (this is Result.Error) {
        action(error)
    }
    return this
}

// Extension to convert throwables to AppError
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