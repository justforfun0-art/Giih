package com.example.gigwork.core.error.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a user-facing error message with associated metadata and actions.
 */
@Parcelize
data class ErrorMessage(
    val message: String,
    val title: String? = null,
    val action: ErrorAction? = null,
    val level: ErrorLevel = ErrorLevel.ERROR,
    val metadata: Map<String, String> = emptyMap(),
    val shouldShow: Boolean = true
) : Parcelable {

    companion object {
        fun networkError(message: String) = ErrorMessage(
            message = message,
            title = "Network Error",
            action = ErrorAction.Retry,
            level = ErrorLevel.ERROR
        )

        fun validationError(message: String) = ErrorMessage(
            message = message,
            level = ErrorLevel.WARNING
        )

        fun criticalError(message: String) = ErrorMessage(
            message = message,
            title = "Critical Error",
            level = ErrorLevel.CRITICAL
        )

        fun info(message: String) = ErrorMessage(
            message = message,
            level = ErrorLevel.INFO
        )
    }
}

/**
 * Builder class for creating ErrorMessage instances with a fluent API.
 */
class ErrorMessageBuilder {
    private var message: String = ""
    private var title: String? = null
    private var action: ErrorAction? = null
    private var level: ErrorLevel = ErrorLevel.ERROR
    private var metadata: MutableMap<String, String> = mutableMapOf()

    fun message(message: String) = apply { this.message = message }
    fun title(title: String) = apply { this.title = title }
    fun action(action: ErrorAction) = apply { this.action = action }
    fun level(level: ErrorLevel) = apply { this.level = level }
    fun metadata(key: String, value: String) = apply { this.metadata[key] = value }

    fun build() = ErrorMessage(
        message = message,
        title = title,
        action = action,
        level = level,
        metadata = metadata.toMap()
    )
}

/**
 * Extension function to create an ErrorMessage using the builder pattern.
 */
fun errorMessage(block: ErrorMessageBuilder.() -> Unit): ErrorMessage {
    return ErrorMessageBuilder().apply(block).build()
}

/**
 * Extension functions for ErrorMessage to check common conditions
 */
fun ErrorMessage.isRetryable(): Boolean = action is ErrorAction.Retry
fun ErrorMessage.isDismissible(): Boolean = action is ErrorAction.Dismiss || level != ErrorLevel.CRITICAL
fun ErrorMessage.hasAction(): Boolean = action != null
fun ErrorMessage.requiresUserAction(): Boolean = level.requiresAcknowledgment()