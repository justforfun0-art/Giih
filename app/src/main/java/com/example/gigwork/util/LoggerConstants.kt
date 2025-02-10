package com.example.gigwork.util

/**
 * Constants used throughout the logging system
 */
object LoggerConstants {
    // General Constants
    const val DEFAULT_TAG = "GigWork"
    const val MAX_TAG_LENGTH = 23
    const val MAX_LOG_LENGTH = 4000
    const val STACK_TRACE_INITIAL_SIZE = 10

    // Tag Prefixes
    const val NETWORK_PREFIX = "NET"
    const val DATABASE_PREFIX = "DB"
    const val UI_PREFIX = "UI"
    const val BUSINESS_PREFIX = "BIZ"
    const val ANALYTICS_PREFIX = "ANALYTICS"
    const val SECURITY_PREFIX = "SEC"

    /**
     * Categories for different types of logs
     */
    object Categories {
        const val NETWORK = "Network"
        const val DATABASE = "Database"
        const val UI = "UI"
        const val BUSINESS = "Business"
        const val ANALYTICS = "Analytics"
        const val SECURITY = "Security"
        const val PERFORMANCE = "Performance"
        const val LIFECYCLE = "Lifecycle"
        const val BACKGROUND = "Background"
        const val CRASH = "Crash"
    }

    /**
     * Subcategories for more specific logging
     */
    object SubCategories {
        // Network subcategories
        const val API = "API"
        const val WEBSOCKET = "WebSocket"
        const val CACHE = "Cache"

        // Database subcategories
        const val QUERY = "Query"
        const val TRANSACTION = "Transaction"
        const val MIGRATION = "Migration"

        // UI subcategories
        const val VIEW = "View"
        const val NAVIGATION = "Navigation"
        const val INTERACTION = "Interaction"

        // Business subcategories
        const val VALIDATION = "Validation"
        const val PROCESSING = "Processing"
        const val CALCULATION = "Calculation"
    }

    /**
     * Error codes for different types of logging failures
     */
    object ErrorCodes {
        const val LOG_WRITE_FAILED = "LOG_001"
        const val TAG_TOO_LONG = "LOG_002"
        const val MESSAGE_TOO_LONG = "LOG_003"
        const val INVALID_LOG_LEVEL = "LOG_004"
    }

    /**
     * Metadata keys for additional logging information
     */
    object MetadataKeys {
        const val TIMESTAMP = "timestamp"
        const val THREAD_ID = "thread_id"
        const val THREAD_NAME = "thread_name"
        const val CLASS_NAME = "class_name"
        const val METHOD_NAME = "method_name"
        const val LINE_NUMBER = "line_number"
        const val DURATION = "duration"
        const val USER_ID = "user_id"
        const val SESSION_ID = "session_id"
        const val BUILD_VERSION = "build_version"
        const val DEVICE_INFO = "device_info"
    }

    /**
     * Format patterns for log messages
     */
    object Patterns {
        const val DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"
        const val LOG_FORMAT = "%s/%s: %s"
        const val METADATA_FORMAT = "[%s: %s]"
    }
}