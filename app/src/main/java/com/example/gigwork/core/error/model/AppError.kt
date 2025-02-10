// core/error/model/AppError.kt
package com.example.gigwork.core.error.model

sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null,
    open val errorCode: String? = null
) {

    companion object {
        // Error code prefixes
        private const val NETWORK_ERROR = "NET"
        private const val DATABASE_ERROR = "DB"
        private const val VALIDATION_ERROR = "VAL"
        private const val SECURITY_ERROR = "SEC"
        private const val BUSINESS_ERROR = "BUS"
        private const val UNEXPECTED_ERROR = "UNX"
        private const val FILE_ERROR = "FILE"
        private const val CACHE_ERROR = "CACHE"
    }

    data class NetworkError(
        override val message: String,
        override val cause: Throwable? = null,
        override val errorCode: String? = null,
        val httpCode: Int? = null,
        val isConnectionError: Boolean = false,
        val url: String? = null,
        val responseBody: String? = null
    ) : AppError(message, cause, errorCode)

    data class DatabaseError(
        override val message: String,
        override val cause: Throwable? = null,
        override val errorCode: String? = null,
        val entity: String? = null,
        val operation: String? = null,
        val sqlError: String? = null
    ) : AppError(message, cause, errorCode)

    data class ValidationError(
        override val message: String,
        override val errorCode: String? = null,
        val field: String? = null,
        val value: Any? = null,
        val constraints: List<String> = emptyList()
    ) : AppError(message, null, errorCode)

    data class SecurityError(
        override val message: String,
        override val cause: Throwable? = null,
        override val errorCode: String? = null,
        val securityDomain: String? = null,
        val requiredPermissions: List<String>? = null
    ) : AppError(message, cause, errorCode)

    data class FileError(
        override val message: String,
        override val cause: Throwable? = null,
        override val errorCode: String? = null,
        val filePath: String? = null,
        val operation: FileOperation? = null
    ) : AppError(message, cause, errorCode) {
        enum class FileOperation {
            READ, WRITE, DELETE, CREATE, MOVE, COPY
        }
    }

    data class CacheError(
        override val message: String,
        override val cause: Throwable? = null,
        override val errorCode: String? = null,
        val key: String? = null,
        val operation: CacheError.CacheOperation? = null
    ) : AppError(message, cause, errorCode) {
        enum class CacheOperation {
            GET, PUT, REMOVE, CLEAR
        }
    }

    data class BusinessError(
        override val message: String,
        override val errorCode: String? = null,
        val domain: String? = null,
        val details: Map<String, Any>? = null
    ) : AppError(message, null, errorCode)

    data class UnexpectedError(
        override val message: String,
        override val cause: Throwable? = null,
        override val errorCode: String? = null,
        val stackTrace: String? = null
    ) : AppError(message, cause, errorCode)

    fun isCritical(): Boolean = when (this) {
        is SecurityError -> true
        is DatabaseError -> operation == "corruption"
        is NetworkError -> httpCode in 500..599
        is UnexpectedError -> true
        else -> false
    }

    fun isRecoverable(): Boolean = when (this) {
        is NetworkError -> isConnectionError || (httpCode in 500..599)
        is DatabaseError -> true
        is FileError -> operation != FileError.FileOperation.DELETE
        is CacheError -> operation != CacheOperation.CLEAR
        is SecurityError -> securityDomain == "authentication"
        else -> false
    }

    fun getUserMessage(): String = when (this) {
        is NetworkError -> if (isConnectionError) {
            "Please check your internet connection and try again"
        } else {
            "Network error: $message"
        }
        is ValidationError -> message
        is SecurityError -> when (securityDomain) {
            "authentication" -> "Please log in to continue"
            "authorization" -> "You don't have permission for this action"
            else -> message
        }
        else -> message
    }
}