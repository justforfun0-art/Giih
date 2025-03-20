package com.example.gigwork.core.error

import com.example.gigwork.core.error.model.AppError
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ExceptionMapper {
    fun map(throwable: Throwable, context: String? = null): AppError {
        return when (throwable) {
            is AppError -> throwable
            is HttpException -> mapHttpException(throwable)
            is SocketTimeoutException -> AppError.NetworkError(
                message = "Connection timed out",
                cause = throwable,
                isConnectionError = true,
                errorCode = "NET_TIMEOUT"
            )
            is UnknownHostException -> AppError.NetworkError(
                message = "Unable to reach server",
                cause = throwable,
                isConnectionError = true,
                errorCode = "NET_HOST_UNREACHABLE"
            )
            else -> AppError.UnexpectedError(
                message = throwable.message ?: "An unexpected error occurred",
                cause = throwable,
                errorCode = "UNX_${context?.uppercase() ?: "GENERIC"}"
            )
        }
    }

    private fun mapHttpException(exception: HttpException): AppError.NetworkError {
        val message = when (exception.code()) {
            401 -> "Unauthorized access"
            403 -> "Access forbidden"
            404 -> "Resource not found"
            in 500..599 -> "Server error occurred"
            else -> exception.message()
        }
        return AppError.NetworkError(
            message = message,
            cause = exception,
            httpCode = exception.code(),
            errorCode = "NET_HTTP_${exception.code()}",
            isConnectionError = false
        )
    }
}