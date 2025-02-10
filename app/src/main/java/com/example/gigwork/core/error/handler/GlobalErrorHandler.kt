package com.example.gigwork.core.error.handler

import android.content.Context
import com.example.gigwork.R
import com.example.gigwork.core.error.model.*
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
) {
    companion object {
        private const val TAG = "GlobalErrorHandler"

        // Analytics event names
        private const val EVENT_ERROR = "error_occurred"
        private const val EVENT_CRITICAL_ERROR = "critical_error"

        // Error categories for analytics
        private const val CATEGORY_NETWORK = "network"
        private const val CATEGORY_DATABASE = "database"
        private const val CATEGORY_VALIDATION = "validation"
        private const val CATEGORY_SECURITY = "security"
        private const val CATEGORY_BUSINESS = "business"
        private const val CATEGORY_UNKNOWN = "unknown"
    }

    fun handle(error: AppError): ErrorMessage {
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

    private fun logError(error: AppError) {
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

    private fun trackError(error: AppError) {
        val eventName = if (error.isCritical()) EVENT_CRITICAL_ERROR else EVENT_ERROR

        val category = when (error) {
            is AppError.NetworkError -> CATEGORY_NETWORK
            is AppError.DatabaseError -> CATEGORY_DATABASE
            is AppError.ValidationError -> CATEGORY_VALIDATION
            is AppError.SecurityError -> CATEGORY_SECURITY
            is AppError.BusinessError -> CATEGORY_BUSINESS
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

    private fun reportToCrashlytics(error: AppError) {
        crashlytics.apply {
            setCustomKey("error_code", error.errorCode ?: "unknown")
            setCustomKey("error_type", error::class.simpleName ?: "unknown")
            setCustomKey("error_message", error.message)

            error.cause?.let { recordException(it) } ?:
            recordException(Exception(error.message))
        }
    }

    private fun createErrorMessage(error: AppError): ErrorMessage {
        return when (error) {
            is AppError.NetworkError -> handleNetworkError(error)
            is AppError.DatabaseError -> handleDatabaseError(error)
            is AppError.ValidationError -> handleValidationError(error)
            is AppError.SecurityError -> handleSecurityError(error)
            is AppError.BusinessError -> handleBusinessError(error)
            else -> handleUnexpectedError(error)
        }
    }

    private fun handleNetworkError(error: AppError.NetworkError): ErrorMessage {
        val message = when {
            error.isConnectionError -> context.getString(R.string.error_no_internet)
            error.httpCode == 401 -> context.getString(R.string.error_unauthorized)
            error.httpCode == 403 -> context.getString(R.string.error_forbidden)
            error.httpCode == 404 -> context.getString(R.string.error_not_found)
            error.httpCode in 500..599 -> context.getString(R.string.error_server)
            else -> error.message
        }

        val action = when {
            error.isConnectionError -> ErrorAction.Multiple(
                primary = ErrorAction.Retry,
                secondary = ErrorAction.Dismiss
            )

            error.httpCode == 401 -> ErrorAction.Custom(
                label = "Login",
                route = "login_screen"
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
            title = context.getString(R.string.error_title_network),
            action = action,
            level = level,
            metadata = mapOf(
                "error_code" to (error.errorCode ?: "unknown"),
                "http_code" to (error.httpCode?.toString() ?: "unknown")
            )
        )
    }

    private fun handleDatabaseError(error: AppError.DatabaseError): ErrorMessage {
        val message = when (error.operation) {
            "insert" -> context.getString(R.string.error_saving_data)
            "query" -> context.getString(R.string.error_loading_data)
            "delete" -> context.getString(R.string.error_deleting_data)
            else -> context.getString(R.string.error_database_generic)
        }

        return ErrorMessage(
            message = message,
            title = context.getString(R.string.error_title_database),
            action = ErrorAction.Multiple(
                primary = ErrorAction.Retry,
                secondary = ErrorAction.Dismiss
            ),
            level = ErrorLevel.ERROR,
            metadata = mapOf(
                "error_code" to (error.errorCode ?: "unknown"),
                "entity" to (error.entity ?: "unknown"),
                "operation" to (error.operation ?: "unknown")
            )
        )
    }

    private fun handleValidationError(error: AppError.ValidationError): ErrorMessage {
        return ErrorMessage(
            message = error.message,
            title = context.getString(R.string.error_title_validation),
            action = ErrorAction.Dismiss,
            level = ErrorLevel.WARNING,
            metadata = mapOf(
                "error_code" to (error.errorCode ?: "unknown"),
                "field" to (error.field ?: "unknown")
            )
        )
    }

    private fun handleSecurityError(error: AppError.SecurityError): ErrorMessage {
        val message = when (error.securityDomain) {
            "authentication" -> context.getString(R.string.error_authentication)
            "authorization" -> context.getString(R.string.error_authorization)
            else -> error.message
        }

        val action = when (error.securityDomain) {
            "authentication" -> ErrorAction.Custom(
                label = "Login",
                route = "login_screen"
            )            else -> ErrorAction.Dismiss
        }

        return ErrorMessage(
            message = message,
            title = context.getString(R.string.error_title_security),
            action = action,
            level = ErrorLevel.ERROR,
            metadata = mapOf(
                "error_code" to (error.errorCode ?: "unknown"),
                "security_domain" to (error.securityDomain ?: "unknown")
            )
        )
    }

    private fun handleBusinessError(error: AppError.BusinessError): ErrorMessage {
        return ErrorMessage(
            message = error.message,
            title = context.getString(R.string.error_title_business),
            action = ErrorAction.Dismiss,
            level = ErrorLevel.WARNING,
            metadata = mapOf(
                "error_code" to (error.errorCode ?: "unknown"),
                "domain" to (error.domain ?: "unknown")
            )
        )
    }

    private fun handleUnexpectedError(error: AppError): ErrorMessage {
        return ErrorMessage(
            message = context.getString(R.string.error_unexpected),
            title = context.getString(R.string.error_title_unexpected),
            action = ErrorAction.Multiple(
                primary = ErrorAction.Retry,
                secondary = ErrorAction.Custom(
                    label = "Contact Support",
                    route = "support_screen",
                    data = mapOf("type" to "support")
                )
            ),
            level = ErrorLevel.CRITICAL,
            metadata = mapOf(
                "error_code" to (error.errorCode ?: "unknown"),
                "stack_trace" to (error.cause?.stackTraceToString() ?: "")
            )
        )
    }

    private fun shouldReportToCrashlytics(error: AppError): Boolean {
        return when (error) {
            is AppError.ValidationError -> false
            is AppError.NetworkError -> error.httpCode in 500..599
            is AppError.SecurityError -> false
            else -> true
        }
    }

    private fun shouldShowToUser(error: AppError): Boolean {
        return when (error) {
            is AppError.ValidationError -> true
            is AppError.NetworkError -> error.isConnectionError || error.httpCode != 401
            is AppError.SecurityError -> error.securityDomain == "authentication"
            else -> true
        }
    }
}