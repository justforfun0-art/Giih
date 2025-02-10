package com.example.gigwork.util.analytics

import javax.inject.Inject

interface Analytics {
    fun trackEvent(name: String, properties: Map<String, Any?>)
}

class AnalyticsImpl @Inject constructor() : Analytics {
    override fun trackEvent(name: String, properties: Map<String, Any?>) {
        // Implement your analytics logic here
    }
}