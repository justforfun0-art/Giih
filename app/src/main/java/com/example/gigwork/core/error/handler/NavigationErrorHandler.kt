// core/error/handler/NavigationErrorHandler.kt
package com.example.gigwork.core.error.handler

import com.example.gigwork.core.error.model.NavigationError
import com.example.gigwork.domain.models.UserRole
import com.example.gigwork.presentation.navigation.NavigationManager
import com.example.gigwork.presentation.navigation.Screen
import com.example.gigwork.util.Logger
import javax.inject.Inject

class NavigationErrorHandler @Inject constructor(
    private val navigationManager: NavigationManager,
    private val logger: Logger
) {
    suspend fun handleError(error: NavigationError, userRole: UserRole) {
        logger.e(
            tag = TAG,
            message = "Navigation error occurred",
            throwable = error,
            additionalData = mapOf("userRole" to userRole.name)
        )

        when (error) {
            is NavigationError.UnauthorizedNavigation -> {
                navigationManager.handleNavigation(
                    route = Screen.Welcome.route,
                    userRole = userRole,
                    clearBackStack = true
                )
            }
            is NavigationError.InvalidDeepLink -> {
                navigationManager.handleNavigation(
                    route = Screen.Welcome.route,
                    userRole = userRole
                )
            }
            is NavigationError.NavigationFailed -> {
                // Attempt to navigate to a safe screen
                navigationManager.handleNavigation(
                    route = Screen.Welcome.route,
                    userRole = userRole
                )
            }
            is NavigationError.InvalidRoute -> {
                navigationManager.handleNavigation(
                    route = Screen.Welcome.route,
                    userRole = userRole
                )
            }
        }
    }

    companion object {
        private const val TAG = "NavigationErrorHandler"
    }
}