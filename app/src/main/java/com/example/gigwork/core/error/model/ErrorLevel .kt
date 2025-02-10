package com.example.gigwork.core.error.model

/**
 * Represents the severity level of an error message.
 * Used to determine how the error should be displayed and handled in the UI.
 */
enum class ErrorLevel {
    /**
     * Informational message that doesn't indicate an error condition.
     * Example: "Settings have been updated successfully"
     */
    INFO,

    /**
     * Warning message that indicates a potential issue but doesn't prevent the app from functioning.
     * Example: "Low storage space available"
     */
    WARNING,

    /**
     * Standard error indicating an operation failed but the app can continue functioning.
     * Example: "Failed to load data, please try again"
     */
    ERROR,

    /**
     * Critical error that requires immediate attention or prevents the app from functioning properly.
     * Example: "Database corruption detected"
     */
    CRITICAL;

    /**
     * Whether this error level should block UI interaction
     */
    fun isBlocking(): Boolean = this == CRITICAL

    /**
     * Whether this error level requires explicit user acknowledgment
     */
    fun requiresAcknowledgment(): Boolean = this == CRITICAL || this == ERROR

    /**
     * Whether this error should be automatically dismissed after a timeout
     */
    fun shouldAutoHide(): Boolean = this == INFO || this == WARNING

    /**
     * Get the default timeout duration in milliseconds for auto-hiding errors
     */
    fun getAutoHideDuration(): Long = when (this) {
        INFO -> 3000L // 3 seconds
        WARNING -> 5000L // 5 seconds
        else -> 0L // Don't auto-hide
    }

    /**
     * Whether this error level should be logged for analytics
     */
    fun shouldLog(): Boolean = this != INFO

    /**
     * Get the analytics priority level
     */
    fun getAnalyticsPriority(): Int = when (this) {
        INFO -> 0
        WARNING -> 1
        ERROR -> 2
        CRITICAL -> 3
    }

    /**
     * Get the color resource to use for this error level
     * These correspond to Material color tokens
     */
    fun getColorToken(): String = when (this) {
        INFO -> "primary"
        WARNING -> "secondary"
        ERROR -> "error"
        CRITICAL -> "error"
    }

    companion object {
        /**
         * Get appropriate error level based on HTTP status code
         */
        fun fromHttpStatus(code: Int): ErrorLevel = when (code) {
            in 200..299 -> INFO
            in 400..499 -> ERROR
            in 500..599 -> CRITICAL
            else -> ERROR
        }

        /**
         * Get appropriate error level for validation errors
         */
        fun forValidation(isBlocking: Boolean): ErrorLevel =
            if (isBlocking) ERROR else WARNING

        /**
         * Get appropriate error level for network errors
         */
        fun forNetworkError(isServerError: Boolean): ErrorLevel =
            if (isServerError) CRITICAL else ERROR
    }
}