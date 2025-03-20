// presentation/navigation/analytics/NavigationEventTracker.kt
package com.example.gigwork.presentation.navigation.analytics


import com.example.gigwork.domain.models.UserRole
import com.example.gigwork.util.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import com.example.gigwork.di.IoDispatcher

interface NavigationEventTracker {
    fun trackNavigationEvent(
        fromScreen: String,
        toScreen: String,
        trigger: String,
        userRole: UserRole
    )
}

@Singleton
class NavigationEventTrackerImpl @Inject constructor(
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : NavigationEventTracker {

    override fun trackNavigationEvent(
        fromScreen: String,
        toScreen: String,
        trigger: String,
        userRole: UserRole
    ) {
        val eventData = mapOf(
            "from_screen" to fromScreen,
            "to_screen" to toScreen,
            "trigger" to trigger,
            "user_role" to userRole.name,
            "timestamp" to System.currentTimeMillis()
        )

        logger.i(
            tag = TAG,
            message = "Navigation event: $fromScreen -> $toScreen",
            additionalData = eventData
        )
    }

    companion object {
        private const val TAG = "NavigationEventTracker"
    }
}