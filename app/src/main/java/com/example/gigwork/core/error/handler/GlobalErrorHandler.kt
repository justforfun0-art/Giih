package com.example.gigwork.core.error.handler

import android.content.Context
import com.example.gigwork.R
import com.example.gigwork.core.error.model.*
import com.example.gigwork.presentation.base.AppError
import com.example.gigwork.presentation.base.ErrorHandler
import com.example.gigwork.util.Logger
import com.example.gigwork.util.analytics.Analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalErrorHandler @Inject constructor(
    private val logger: Logger,
    private val analytics: Analytics,
    private val crashlytics: FirebaseCrashlytics,
    @ApplicationContext private val context: Context
) : ErrorHandler {

    companion object {
        private const val TAG = "GlobalErrorHandler"
        private const val EVENT_ERROR = "error_occurred"
        private const val EVENT_CRITICAL_ERROR = "critical_error"
        private const val CATEGORY_NETWORK = "network"
        private const val CATEGORY_DATABASE = "database"
        private const val CATEGORY_VALIDATION = "validation"
        private const val CATEGORY_SECURITY = "security"
        private const val CATEGORY_BUSINESS = "business"
        private const val CATEGORY_UNKNOWN = "unknown"
    }

    override fun handle(error: AppError): ErrorMessage {
        val coreError = convertToCoreError(error)
        logError(coreError)
        trackError(coreError)

        if (shouldReportToCrashlytics(coreError)) {
            reportToCrashlytics(coreError)
        }

        return if (shouldShowToUser(coreError)) {
            createErrorMessage(coreError)
        } else {
            ErrorMessage(
                message = coreError.message,
                level = ErrorLevel.INFO,
                action = ErrorAction.Dismiss,
                shouldShow = false
            )
        }
    }

    fun handleCoreError(error: com.example.gigwork.core.error.model.AppError): ErrorMessage {
        logError(error)
        trackError(error)

        if (shouldReportToCrashlytics(error)) {
            reportToCrashlytics(error)
        }

        return if (shouldShowToUser(error)) {
            createErrorMessage(error)
        } else {
            ErrorMessage(
                message = error.message,
                level = ErrorLevel.INFO,
                action = ErrorAction.Dismiss,
                shouldShow = false
            )
        }
    }

    private fun convertToCoreError(error: AppError): com.example.gigwork.core.error.model.AppError {
        return when (error) {
            is AppError.Api -> com.example.gigwork.core.error.model.AppError.NetworkError(
                message = error.message,
                cause = error.cause,
                httpCode = error.code,
                errorCode = "NET_${error.code}"
            )
            is AppError.Network -> com.example.gigwork.core.error.model.AppError.NetworkError(
                message = error.message,
                cause = error.cause,
                isConnectionError = true,
                errorCode = "NET_CONN"
            )
            is AppError.Database -> com.example.gigwork.core.error.model.AppError.DatabaseError(
                message = error.message,
                cause = error.cause,
                errorCode = "DB_ERR"
            )
            is AppError.Validation -> com.example.gigwork.core.error.model.AppError.ValidationError(
                message = error.message,
                field = error.errors.keys.firstOrNull(),
                errorCode = "VAL_ERR"
            )
            is AppError.UnexpectedError -> com.example.gigwork.core.error.model.AppError.UnexpectedError(
                message = error.message,
                cause = error.cause,
                errorCode = "UNX_ERR"
            )
        }
    }

    private fun logError(error: com.example.gigwork.core.error.model.AppError) {
        val errorData = mapOf(
            "error_code" to (error.errorCode ?: "unknown"),
            "error_type" to error::class.simpleName,
            "message" to error.message,
            "stacktrace" to error.cause?.stackTraceToString()
        )

        logger.e(
            tag = TAG,
            message = "Error occurred: ${error.message}",
            throwable = error.cause,
            additionalData = errorData
        )
    }

    private fun trackError(error: com.example.gigwork.core.error.model.AppError) {
        val eventName = if (error.isCritical()) EVENT_CRITICAL_ERROR else EVENT_ERROR

        val category = when (error) {
            is com.example.gigwork.core.error.model.AppError.NetworkError -> CATEGORY_NETWORK
            is com.example.gigwork.core.error.model.AppError.DatabaseError -> CATEGORY_DATABASE
            is com.example.gigwork.core.error.model.AppError.ValidationError -> CATEGORY_VALIDATION
            is com.example.gigwork.core.error.model.AppError.SecurityError -> CATEGORY_SECURITY
            is com.example.gigwork.core.error.model.AppError.BusinessError -> CATEGORY_BUSINESS
            else -> CATEGORY_UNKNOWN
        }

        analytics.trackEvent(
            name = eventName,
            properties = mapOf(
                "category" to category,
                "error_code" to (error.errorCode ?: "unknown"),
                "error_type" to error::class.simpleName,
                "message" to error.message,
                "is_critical" to error.isCritical()
            )
        )
    }

    private fun reportToCrashlytics(error: com.example.gigwork.core.error.model.AppError) {
        crashlytics.apply {
            setCustomKey("error_code", error.errorCode ?: "unknown")
            setCustomKey("error_type", error::class.simpleName ?: "unknown")
            setCustomKey("error_message", error.message)

            error.cause?.let { recordException(it) } ?:
            recordException(Exception(error.message))
        }
    }

    private fun createErrorMessage(error: com.example.gigwork.core.error.model.AppError): ErrorMessage {
        return when (error) {
            is com.example.gigwork.core.error.model.AppError.NetworkError -> handleNetworkError(error)
            is com.example.gigwork.core.error.model.AppError.DatabaseError -> handleDatabaseError(error)
            is com.example.gigwork.core.error.model.AppError.ValidationError -> handleValidationError(error)
            is com.example.gigwork.core.error.model.AppError.SecurityError -> handleSecurityError(error)
            is com.example.gigwork.core.error.model.AppError.BusinessError -> handleBusinessError(error)
            else -> handleUnexpectedError(error)
        }
    }
    private fun handleNetworkError(error: com.example.gigwork.core.error.model.AppError.NetworkError): ErrorMessage {
        val message = when {
            error.isConnectionError -> "Please check your internet connection"
            error.httpCode == 401 -> "Unauthorized access. Please login again"
            error.httpCode == 403 -> "You don't have permission to access this resource"
            error.httpCode == 404 -> "The requested resource was not found"
            error.httpCode in 500..599 -> "A server error occurred. Please try again later"
            else -> error.message
        }

        val action = when {
            error.isConnectionError -> {
                val retryAction = ErrorAction.Retry
                val dismissAction = ErrorAction.Dismiss
                ErrorAction.Multiple(
                    primary = retryAction,
                    secondary = dismissAction
                )
            }
            error.httpCode == 401 -> ErrorAction.Custom(
                label = "Login",
                route = "login_screen",
                data = emptyMap()
            )
            error.httpCode == 403 -> ErrorAction.Dismiss
            else -> ErrorAction.Retry
        }

        val level = when {
            error.httpCode in 500..599 -> ErrorLevel.CRITICAL
            error.isConnectionError -> ErrorLevel.WARNING
            else -> ErrorLevel.ERROR
        }

        return ErrorMessage(
            message = message,
            title = "Network Error",
            action = action,
            level = level,
            shouldShow = true,
            metadata = mapOf(
                "error_code" to (error.errorCode ?: "unknown"),
                "http_code" to (error.httpCode?.toString() ?: "unknown")
            )
        )
    }

    private fun handleDatabaseError(error: com.example.gigwork.core.error.model.AppError.DatabaseError): ErrorMessage {
        val message = when (error.operation) {
            "insert" -> "Failed to save data to database"
            "query" -> "Failed to load data from database"
            "delete" -> "Failed to delete data from database"
            else -> "A database error occurred"
        }

        val retryAction = ErrorAction.Retry
        val dismissAction = ErrorAction.Dismiss
        val combinedAction = ErrorAction.Multiple(
            primary = retryAction,
            secondary = dismissAction
        )

        return ErrorMessage(
            message = message,
            title = "Database Error",
            action = combinedAction,
            level = ErrorLevel.ERROR,
            shouldShow = true,
            metadata = mapOf(
                "error_code" to (error.errorCode ?: "unknown"),
                "entity" to (error.entity ?: "unknown"),
                "operation" to (error.operation ?: "unknown")
            )
        )
    }
    private fun handleValidationError(error: com.example.gigwork.core.error.model.AppError.ValidationError): ErrorMessage {
        return ErrorMessage(
            message = error.message,
            title = "Validation Error",
            action = ErrorAction.Dismiss,
            level = ErrorLevel.WARNING,
            shouldShow = true,
            metadata = mapOf(
                "error_code" to (error.errorCode ?: "unknown"),
                "field" to (error.field ?: "unknown"),
                "constraints" to (error.constraints.joinToString(","))
            )
        )
    }

    private fun handleSecurityError(error: com.example.gigwork.core.error.model.AppError.SecurityError): ErrorMessage {
        val message = when (error.securityDomain) {
            "authentication" -> "Please login to continue"
            "authorization" -> "You don't have permission for this action"
            else -> error.message
        }

        val action = when (error.securityDomain) {
            "authentication" -> ErrorAction.Custom(
                label = "Login",
                route = "login_screen",
                data = emptyMap()
            )
            else -> ErrorAction.Dismiss
        }

        return ErrorMessage(
            message = message,
            title = "Security Error",
            action = action,
            level = ErrorLevel.ERROR,
            shouldShow = true,
            metadata = mapOf(
                "error_code" to (error.errorCode ?: "unknown"),
                "security_domain" to (error.securityDomain ?: "unknown")
            )
        )
    }

    private fun handleBusinessError(error: com.example.gigwork.core.error.model.AppError.BusinessError): ErrorMessage {
        return ErrorMessage(
            message = error.message,
            title = "Business Error",
            action = ErrorAction.Dismiss,
            level = ErrorLevel.WARNING,
            shouldShow = true,
            metadata = mapOf(
                "error_code" to (error.errorCode ?: "unknown"),
                "domain" to (error.domain ?: "unknown")
            )
        )
    }

    private fun handleUnexpectedError(error: com.example.gigwork.core.error.model.AppError): ErrorMessage {
        val retryAction = ErrorAction.Retry
        val supportAction = ErrorAction.Custom(
            label = "Contact Support",
            route = "support_screen",
            data = emptyMap()
        )

        return ErrorMessage(
            message = "An unexpected error occurred. Please try again or contact support.",
            title = "Unexpected Error",
            action = ErrorAction.Multiple(
                primary = retryAction,
                secondary = supportAction
            ),
            level = ErrorLevel.CRITICAL,
            shouldShow = true,
            metadata = mapOf(
                "error_code" to (error.errorCode ?: "unknown"),
                "stack_trace" to (error.cause?.stackTraceToString() ?: "")
            )
        )
    }

    private fun shouldReportToCrashlytics(error: com.example.gigwork.core.error.model.AppError): Boolean {
        return when (error) {
            is com.example.gigwork.core.error.model.AppError.ValidationError -> false
            is com.example.gigwork.core.error.model.AppError.NetworkError -> error.httpCode in 500..599
            is com.example.gigwork.core.error.model.AppError.SecurityError -> false
            else -> true
        }
    }

    private fun shouldShowToUser(error: com.example.gigwork.core.error.model.AppError): Boolean {
        return when (error) {
            is com.example.gigwork.core.error.model.AppError.ValidationError -> true
            is com.example.gigwork.core.error.model.AppError.NetworkError ->
                error.isConnectionError || error.httpCode != 401
            is com.example.gigwork.core.error.model.AppError.SecurityError ->
                error.securityDomain == "authentication"
            else -> true
        }
    }
}