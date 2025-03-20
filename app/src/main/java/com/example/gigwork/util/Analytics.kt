package com.example.gigwork.util.analytics

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface defining analytics tracking operations throughout the app
 */
interface Analytics {
    /**
     * Track a custom event with optional properties
     */
    fun trackEvent(name: String, properties: Map<String, Any?> = emptyMap())

    /**
     * Track screen view events
     */
    fun trackScreenView(screenName: String, screenClass: String)

    /**
     * Track user properties
     */
    fun setUserProperty(name: String, value: String?)

    /**
     * Set user identifier
     */
    fun setUserId(id: String?)

    /**
     * Log app errors for analytics
     */
    fun logError(error: Throwable, additionalInfo: Map<String, Any?> = emptyMap())
}

/**
 * Implementation of Analytics that sends data to Firebase Analytics
 */
@Singleton
class FirebaseAnalyticsImpl @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) : Analytics {

    companion object {
        private const val TAG = "FirebaseAnalytics"
        private const val MAX_PROPERTY_LENGTH = 100
        private const val EVENT_ERROR = "app_error"
    }

    override fun trackEvent(name: String, properties: Map<String, Any?>) {
        try {
            val bundle = Bundle()

            // Convert properties to Bundle format
            properties.forEach { (key, value) ->
                when (value) {
                    null -> return@forEach  // Skip null values
                    is String -> bundle.putString(key, value.take(MAX_PROPERTY_LENGTH))
                    is Int -> bundle.putInt(key, value)
                    is Long -> bundle.putLong(key, value)
                    is Double -> bundle.putDouble(key, value)
                    is Float -> bundle.putFloat(key, value)
                    is Boolean -> bundle.putBoolean(key, value)
                    else -> bundle.putString(key, value.toString().take(MAX_PROPERTY_LENGTH))
                }
            }

            // Log the event
            firebaseAnalytics.logEvent(name, bundle)
            Log.d(TAG, "Tracked event: $name with properties: $properties")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track event: $name", e)
        }
    }

    override fun trackScreenView(screenName: String, screenClass: String) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            }

            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            Log.d(TAG, "Tracked screen view: $screenName, class: $screenClass")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track screen view: $screenName", e)
        }
    }

    override fun setUserProperty(name: String, value: String?) {
        try {
            firebaseAnalytics.setUserProperty(name, value)
            Log.d(TAG, "Set user property: $name = $value")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user property: $name", e)
        }
    }

    override fun setUserId(id: String?) {
        try {
            firebaseAnalytics.setUserId(id)
            Log.d(TAG, "Set user ID: $id")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user ID", e)
        }
    }

    override fun logError(error: Throwable, additionalInfo: Map<String, Any?>) {
        val properties = mutableMapOf<String, Any?>(
            "error_message" to (error.message ?: "Unknown error"),
            "error_type" to error.javaClass.simpleName,
            "stack_trace" to error.stackTraceToString().take(500) // Limit length
        ).apply { putAll(additionalInfo) }

        trackEvent(EVENT_ERROR, properties)
    }
}

/**
 * Implementation for debug/development environments that just logs to console
 */
@Singleton
class DebugAnalyticsImpl @Inject constructor() : Analytics {

    companion object {
        private const val TAG = "DebugAnalytics"
    }

    override fun trackEvent(name: String, properties: Map<String, Any?>) {
        Log.d(TAG, "Event: $name, Properties: $properties")
    }

    override fun trackScreenView(screenName: String, screenClass: String) {
        Log.d(TAG, "Screen View: $screenName, Class: $screenClass")
    }

    override fun setUserProperty(name: String, value: String?) {
        Log.d(TAG, "User Property: $name = $value")
    }

    override fun setUserId(id: String?) {
        Log.d(TAG, "User ID: $id")
    }

    override fun logError(error: Throwable, additionalInfo: Map<String, Any?>) {
        Log.e(TAG, "Error: ${error.message}, Additional Info: $additionalInfo", error)
    }
}

/**
 * No-op implementation for testing or when analytics should be disabled
 */
class NoOpAnalyticsImpl : Analytics {
    override fun trackEvent(name: String, properties: Map<String, Any?>) {}
    override fun trackScreenView(screenName: String, screenClass: String) {}
    override fun setUserProperty(name: String, value: String?) {}
    override fun setUserId(id: String?) {}
    override fun logError(error: Throwable, additionalInfo: Map<String, Any?>) {}
}