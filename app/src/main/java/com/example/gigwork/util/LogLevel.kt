package com.example.gigwork.util

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LogLevel

/**
 * Defines the different logging levels available in the application
 */
enum class LogLevel(val value: Int) {
    VERBOSE(1),
    DEBUG(2),
    INFO(3),
    WARN(4),
    ERROR(5),
    ASSERT(6);

    companion object {
        fun fromValue(value: Int): LogLevel {
            return values().firstOrNull { it.value == value } ?: INFO
        }

        fun shouldLog(currentLevel: LogLevel, messageLevel: LogLevel): Boolean {
            return messageLevel.value >= currentLevel.value
        }
    }
}

/**
 * Marker annotations for logging levels
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Verbose

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Debug

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Info

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Warn

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Error

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Assert