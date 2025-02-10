package com.example.gigwork.util

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logger interface defining the contract for logging operations
 */
interface Logger {
    fun d(tag: String, message: String, additionalData: Map<String, Any?> = emptyMap())
    fun i(tag: String, message: String, additionalData: Map<String, Any?> = emptyMap())
    fun w(tag: String, message: String, throwable: Throwable? = null, additionalData: Map<String, Any?> = emptyMap())
    fun e(tag: String, message: String, throwable: Throwable? = null, additionalData: Map<String, Any?> = emptyMap())
}

/**
 * Implementation of Logger interface that provides consistent logging across the app.
 * Handles different log levels and automatic tag generation.
 */
@Singleton
class LoggerImpl @Inject constructor() : Logger {

    companion object {
        private const val TAG_PREFIX = "GigWork_"
        private const val MAX_TAG_LENGTH = 23
        private const val MAX_LOG_LENGTH = 4000

        fun createTag(className: String): String {
            return "$TAG_PREFIX${className.takeLastWhile { it != '.' }}"
                .take(MAX_TAG_LENGTH)
        }
    }

    private val isDebug = true // TODO: Link to BuildConfig.DEBUG

    override fun d(
        tag: String,
        message: String,
        additionalData: Map<String, Any?>
    ) {
        if (isDebug) {
            logMessage(Log.DEBUG, tag, message, null, additionalData)
        }
    }

    override fun i(
        tag: String,
        message: String,
        additionalData: Map<String, Any?>
    ) {
        logMessage(Log.INFO, tag, message, null, additionalData)
    }

    override fun w(
        tag: String,
        message: String,
        throwable: Throwable?,
        additionalData: Map<String, Any?>
    ) {
        logMessage(Log.WARN, tag, message, throwable, additionalData)
    }

    override fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
        additionalData: Map<String, Any?>
    ) {
        logMessage(Log.ERROR, tag, message, throwable, additionalData)
    }

    private fun logMessage(
        priority: Int,
        tag: String,
        message: String,
        throwable: Throwable?,
        additionalData: Map<String, Any?>
    ) {
        val fullMessage = buildString {
            append(message)
            if (additionalData.isNotEmpty()) {
                append("\nAdditional Data: ")
                append(additionalData.toString())
            }
            throwable?.let {
                append("\nException: ${it.message}")
                append("\nStacktrace: ${it.stackTraceToString()}")
            }
        }

        // Split log message if it exceeds maximum length
        if (fullMessage.length > MAX_LOG_LENGTH) {
            val chunks = fullMessage.chunked(MAX_LOG_LENGTH)
            chunks.forEachIndexed { index, chunk ->
                Log.println(priority, tag, "($index/${chunks.size}) $chunk")
            }
        } else {
            Log.println(priority, tag, fullMessage)
        }
    }
}
