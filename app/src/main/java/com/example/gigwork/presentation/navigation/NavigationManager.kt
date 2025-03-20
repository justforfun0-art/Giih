package com.example.gigwork.presentation.navigation

import android.net.Uri
import androidx.navigation.NavOptionsBuilder
import com.example.gigwork.domain.models.UserRole
import com.example.gigwork.presentation.navigation.analytics.NavigationEventTracker
import com.example.gigwork.presentation.navigation.deeplink.DeepLinkHandler
import com.example.gigwork.presentation.navigation.menu.MenuInteractionHandler
import com.example.gigwork.presentation.navigation.permissions.NavigationPermissionManager
import com.example.gigwork.presentation.navigation.state.NavigationState
import com.example.gigwork.presentation.navigation.transitions.ScreenTransitionManager
import com.example.gigwork.util.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import com.example.gigwork.di.IoDispatcher
import kotlinx.coroutines.withContext

@Singleton
class NavigationManager @Inject constructor(
    private val navigationState: NavigationState,
    private val transitionManager: ScreenTransitionManager,
    private val permissionManager: NavigationPermissionManager,
    private val eventTracker: NavigationEventTracker,
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    // Notice we've removed deepLinkHandler and menuHandler from constructor

    suspend fun handleNavigation(
        route: String,
        userRole: UserRole,
        parameters: Map<String, String> = emptyMap(),
        trigger: NavigationTrigger = NavigationTrigger.USER_ACTION,
        clearBackStack: Boolean = false
    ) {
        try {
            val logData = mapOf(
                "userRole" to userRole.name,
                "parameters" to parameters.toString(),
                "trigger" to trigger.name
            )

            logger.d(
                tag = TAG,
                message = "Handling navigation to $route",
                additionalData = logData as Map<String, Any?>
            )

            if (!permissionManager.canNavigateToRoute(route, userRole)) {
                handleUnauthorizedAccess(route, userRole)
                return
            }

            val currentRoute = navigationState.currentRoute.value ?: "unknown"
            performNavigation(route, clearBackStack)

            eventTracker.trackNavigationEvent(
                fromScreen = currentRoute,
                toScreen = route,
                trigger = trigger.name,
                userRole = userRole
            )
        } catch (e: Exception) {
            handleNavigationError(route, e, userRole)
        }
    }

    private suspend fun performNavigation(route: String, clearBackStack: Boolean) {
        val options: NavOptionsBuilder.() -> Unit = {
            if (clearBackStack) {
                popUpTo(0) { inclusive = true }
            }
            launchSingleTop = true
            restoreState = true
        }

        navigationState.navigateTo(route, options)
    }

    private suspend fun handleUnauthorizedAccess(route: String, userRole: UserRole) {
        val logData = mapOf(
            "route" to route,
            "current_route" to (navigationState.currentRoute.value ?: "unknown"),
            "userRole" to userRole.name
        )

        logger.w(
            tag = TAG,
            message = "Unauthorized access attempt",
            additionalData = logData as Map<String, Any?>
        )

        val currentRoute = navigationState.currentRoute.value ?: "unknown"
        eventTracker.trackNavigationEvent(
            fromScreen = currentRoute,
            toScreen = route,
            trigger = "unauthorized_access",
            userRole = userRole
        )

        handleNavigation(
            route = Screen.Welcome.route,
            userRole = userRole,
            clearBackStack = true
        )
    }

    private fun handleNavigationError(route: String, error: Exception, userRole: UserRole) {
        val logData = mapOf(
            "route" to route,
            "error_type" to error.javaClass.simpleName,
            "userRole" to userRole.name
        )

        logger.e(
            tag = TAG,
            message = "Navigation failed",
            throwable = error,
            additionalData = logData as Map<String, Any?>
        )

        val currentRoute = navigationState.currentRoute.value ?: "unknown"
        eventTracker.trackNavigationEvent(
            fromScreen = currentRoute,
            toScreen = route,
            trigger = "error",
            userRole = userRole
        )
    }

    companion object {
        private const val TAG = "NavigationManager"
    }
}