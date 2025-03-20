package com.example.gigwork.presentation.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.gigwork.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for tracking analytics events.
 */
interface AnalyticsTracker {
    /**
     * Track an analytics event with properties.
     */
    fun trackEvent(eventName: String, properties: Map<String, Any> = emptyMap())

    /**
     * Track a screen view.
     */
    fun trackScreenView(screenName: String, properties: Map<String, Any> = emptyMap())
}

/**
 * Implementation of analytics tracking.
 */
@Singleton
class AnalyticsTrackerImpl @Inject constructor(
    private val logger: Logger
) : AnalyticsTracker {

    override fun trackEvent(eventName: String, properties: Map<String, Any>) {
        logger.d(
            tag = TAG,
            message = "Analytics event: $eventName",
            additionalData = properties
        )
        // Implementation would send events to analytics service
    }

    override fun trackScreenView(screenName: String, properties: Map<String, Any>) {
        logger.d(
            tag = TAG,
            message = "Screen view: $screenName",
            additionalData = properties
        )
        // Implementation would send screen view to analytics service
    }

    companion object {
        private const val TAG = "AnalyticsTracker"
    }
}

/**
 * Composable function to remember an analytics tracker instance.
 */
@Composable
fun rememberAnalyticsTracker(): AnalyticsTracker {
    return remember { FakeAnalyticsTracker() }
}

/**
 * Fake implementation for use in previews or tests.
 */
class FakeAnalyticsTracker : AnalyticsTracker {
    override fun trackEvent(eventName: String, properties: Map<String, Any>) {
        // No-op implementation for previews/tests
    }

    override fun trackScreenView(screenName: String, properties: Map<String, Any>) {
        // No-op implementation for previews/tests
    }
}