package com.example.gigwork.core.error.extensions

import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.error.model.ErrorAction
import com.example.gigwork.core.error.model.ErrorLevel
import com.example.gigwork.core.error.model.ErrorMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// First, let's define the ValidationError class that was missing
data class ValidationError(
    val message: String,
    val field: String? = null
)

/**
 * Extension function to convert exceptions to AppError
 */
fun Throwable.toAppError(): AppError {
    return when (this) {
        is AppError -> this
        is HttpException -> handleHttpException(this)
        is IOException -> handleIOException(this)
        is SecurityException -> AppError.SecurityError(
            message = message ?: "Security error occurred",
            cause = this,
            securityDomain = "authentication"
        )
        else -> AppError.UnexpectedError(
            message = message ?: "An unexpected error occurred",
            cause = this
        )
    }
}

// HTTP Exception Handler
private fun handleHttpException(exception: HttpException): AppError {
    return when (exception.code()) {
        401 -> AppError.SecurityError(
            message = "Session expired. Please login again.",
            cause = exception,
            securityDomain = "authentication",
            errorCode = "AUTH_001"
        )
        403 -> AppError.SecurityError(
            message = "You don't have permission to perform this action.",
            cause = exception,
            securityDomain = "authorization",
            errorCode = "AUTH_002"
        )
        404 -> AppError.NetworkError(
            message = "Resource not found.",
            cause = exception,
            httpCode = 404,
            errorCode = "NET_001"
        )
        in 500..599 -> AppError.NetworkError(
            message = "Server error occurred. Please try again later.",
            cause = exception,
            httpCode = exception.code(),
            errorCode = "NET_002"
        )
        else -> AppError.NetworkError(
            message = exception.message(),
            cause = exception,
            httpCode = exception.code(),
            errorCode = "NET_003"
        )
    }
}

// IO Exception Handler
private fun handleIOException(exception: IOException): AppError {
    return when (exception) {
        is SocketTimeoutException -> AppError.NetworkError(
            message = "Connection timed out. Please try again.",
            cause = exception,
            isConnectionError = true,
            errorCode = "NET_004"
        )
        is UnknownHostException -> AppError.NetworkError(
            message = "Unable to connect to server. Please check your internet connection.",
            cause = exception,
            isConnectionError = true,
            errorCode = "NET_005"
        )
        else -> AppError.NetworkError(
            message = "Network error occurred. Please try again.",
            cause = exception,
            isConnectionError = true,
            errorCode = "NET_006"
        )
    }
}

/**
 * Convert ValidationError to AppError
 */
fun ValidationError.toAppError(): AppError.ValidationError {
    return AppError.ValidationError(
        message = message,
        field = field,
        errorCode = "VAL_${field?.uppercase() ?: "001"}"
    )
}

/**
 * Extension function to map a Flow to Result with error handling
 */
fun <T> Flow<T>.asResult(): Flow<Result<T>> {
    return this
        .map<T, Result<T>> { Result.Success(it) }
        .onStart { emit(Result.Loading) }
        .catch { emit(Result.Error(it.toAppError())) }
}

/**
 * Extension function to create ErrorMessage from AppError
 */
fun AppError.toErrorMessage(): ErrorMessage {
    return when (this) {
        is AppError.NetworkError -> {
            if (isConnectionError) {
                ErrorMessage(
                    message = message,
                    title = "Connection Error",
                    level = ErrorLevel.ERROR,
                    action = ErrorAction.Multiple(
                        primary = ErrorAction.Retry,
                        secondary = ErrorAction.Dismiss
                    ),
                    metadata = mapOf(
                        "error_code" to (errorCode ?: "unknown"),
                        "is_connection_error" to "true"
                    )
                )
            } else {
                ErrorMessage(
                    message = message,
                    title = "Network Error",
                    level = if (httpCode in 500..599) ErrorLevel.CRITICAL else ErrorLevel.ERROR,
                    action = ErrorAction.Retry,
                    metadata = mapOf(
                        "error_code" to (errorCode ?: "unknown"),
                        "http_code" to (httpCode?.toString() ?: "unknown")
                    )
                )
            }
        }
        is AppError.ValidationError -> ErrorMessage(
            message = message,
            title = "Validation Error",
            level = ErrorLevel.WARNING,
            action = ErrorAction.Dismiss,
            metadata = mapOf(
                "error_code" to (errorCode ?: "unknown"),
                "field" to (field ?: "unknown")
            )
        )
        is AppError.SecurityError -> ErrorMessage(
            message = message,
            title = "Security Error",
            level = ErrorLevel.CRITICAL,
            action = when (securityDomain) {
                "authentication" -> ErrorAction.Custom(
                    label = "Login",
                    route = "login_screen"
                )
                else -> ErrorAction.Dismiss
            },
            metadata = mapOf(
                "error_code" to (errorCode ?: "unknown"),
                "security_domain" to (securityDomain ?: "unknown")
            )
        )
        is AppError.DatabaseError -> ErrorMessage(
            message = message,
            title = "Database Error",
            level = ErrorLevel.ERROR,
            action = ErrorAction.Multiple(
                primary = ErrorAction.Retry,
                secondary = ErrorAction.Custom(
                    label = "Contact Support",
                    route = "support_screen",
                    data = mapOf("type" to "support")
                )
            ),
            metadata = mapOf(
                "error_code" to (errorCode ?: "unknown"),
                "entity" to (entity ?: "unknown"),
                "operation" to (operation ?: "unknown")
            )
        )
        else -> ErrorMessage(
            message = message,
            title = "Error",
            level = ErrorLevel.ERROR,
            action = ErrorAction.Dismiss,
            metadata = mapOf(
                "error_code" to (errorCode ?: "unknown"),
                "error_type" to this::class.simpleName.toString()
            )
        )
    }
}

/**
 * Result sealed class
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: AppError) : Result<Nothing>()
    object Loading : Result<Nothing>()
}