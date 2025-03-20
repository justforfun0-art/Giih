package com.example.gigwork.presentation.navigation.menu

import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.UserRole
import com.example.gigwork.presentation.navigation.NavigationCommands
import com.example.gigwork.presentation.NavigationScope
import com.example.gigwork.presentation.navigation.NavigationTrigger
import com.example.gigwork.presentation.navigation.Screen
import com.example.gigwork.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuInteractionHandler @Inject constructor(
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @NavigationScope private val navigationScope: CoroutineScope,
    private val navigationCommands: NavigationCommands
) {
    suspend fun handleMenuAction(action: String, userRole: UserRole) {
        try {
            logger.d(
                tag = TAG,
                message = "Processing menu action",
                additionalData = mapOf(
                    "action" to action,
                    "userRole" to userRole.name
                )
            )

            val route = getRouteForAction(action, userRole)
            navigationCommands.navigateTo(route)
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Menu action processing failed",
                throwable = e,
                additionalData = mapOf(
                    "action" to action,
                    "userRole" to userRole.name
                )
            )
            throw e
        }
    }

    // Keep the rest of your methods the same
    private fun getRouteForAction(action: String, userRole: UserRole): String {
        return when (action) {
            MenuAction.PROFILE -> getProfileRoute(userRole)
            MenuAction.DASHBOARD -> getDashboardRoute(userRole)
            MenuAction.JOBS -> Screen.Jobs.route
            MenuAction.SETTINGS -> Screen.Settings.route
            else -> throw IllegalArgumentException("Unknown menu action: $action")
        }
    }

    private fun getProfileRoute(userRole: UserRole): String {
        return when (userRole) {
            UserRole.EMPLOYER -> Screen.EmployerProfile.route
            UserRole.EMPLOYEE -> Screen.EmployeeProfile.route
            else -> Screen.Welcome.route
        }
    }

    private fun getDashboardRoute(userRole: UserRole): String {
        return when (userRole) {
            UserRole.EMPLOYER -> Screen.EmployerDashboard.route
            UserRole.EMPLOYEE -> Screen.Jobs.route
            else -> Screen.Welcome.route
        }
    }

    companion object {
        private const val TAG = "MenuInteractionHandler"

        object MenuAction {
            const val PROFILE = "profile"
            const val DASHBOARD = "dashboard"
            const val JOBS = "jobs"
            const val SETTINGS = "settings"
        }
    }
}