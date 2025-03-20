package com.example.gigwork.presentation.navigation.auth

import com.example.gigwork.core.error.model.ErrorAction
import com.example.gigwork.core.error.model.ErrorLevel
import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.core.error.model.NavigationError
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.UserRole
import com.example.gigwork.domain.models.UserType
import com.example.gigwork.presentation.NavigationScope
import com.example.gigwork.presentation.navigation.AuthRoutes
import com.example.gigwork.presentation.navigation.NavigationCommands
import com.example.gigwork.presentation.navigation.NavigationManager
import com.example.gigwork.presentation.navigation.NavigationTrigger
import com.example.gigwork.presentation.navigation.Screen
import com.example.gigwork.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages auth-specific navigation flows and integrates with the main NavigationManager
 * This class helps maintain a clean separation of concerns for authentication flows
 */
@Singleton
class AuthNavigationManager @Inject constructor(
    private val navigationManager: NavigationManager,
    private val navigationCommands: NavigationCommands,
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @NavigationScope private val navigationScope: CoroutineScope
) {
    /**
     * Start the authentication flow from the beginning (phone input)
     */
    suspend fun startAuthFlow(clearBackStack: Boolean = false) {
        navigationManager.handleNavigation(
            route = AuthRoutes.phoneInput(),
            userRole = UserRole.GUEST,
            trigger = NavigationTrigger.SYSTEM,
            clearBackStack = clearBackStack
        )
    }

    /**
     * Non-suspending version for use from regular functions
     */
    fun startAuthFlowAsync(clearBackStack: Boolean = false) {
        navigationScope.launch {
            startAuthFlow(clearBackStack)
        }
    }

    /**
     * Navigate to OTP verification screen with a phone number
     */
    suspend fun navigateToOtpVerification(phoneNumber: String) {
        val route = AuthRoutes.otpVerification(phoneNumber)

        navigationManager.handleNavigation(
            route = route,
            userRole = UserRole.GUEST,
            trigger = NavigationTrigger.SYSTEM
        )
    }

    /**
     * Navigate to profile completion after OTP verification
     */
    suspend fun navigateToProfileCompletion() {
        navigationManager.handleNavigation(
            route = AuthRoutes.profileCompletion(),
            userRole = UserRole.GUEST,
            trigger = NavigationTrigger.SYSTEM
        )
    }

    /**
     * Complete the auth flow and navigate to the appropriate screen based on user role
     */
    suspend fun completeAuthFlow(userRole: UserRole) {
        val destination = when (userRole) {
            UserRole.EMPLOYER -> Screen.EmployerDashboard.route
            UserRole.EMPLOYEE -> Screen.Jobs.route
            UserRole.GUEST -> Screen.Welcome.route
        }

        navigationManager.handleNavigation(
            route = destination,
            userRole = userRole,
            trigger = NavigationTrigger.SYSTEM,
            clearBackStack = true
        )
    }

    /**
     * Handle authentication errors by showing appropriate screens
     */
    suspend fun handleAuthError(
        error: Throwable,
        userRole: UserRole = UserRole.GUEST
    ) {
        logger.e(
            tag = TAG,
            message = "Authentication error",
            throwable = error,
            additionalData = mapOf("userRole" to userRole.name)
        )

        // Determine error type and navigate accordingly
        when (error) {
            is NavigationError.UnauthorizedNavigation -> {
                startAuthFlow(clearBackStack = true)
            }
            else -> {
                // Default handling - back to welcome screen
                navigationManager.handleNavigation(
                    route = Screen.Welcome.route,
                    userRole = userRole,
                    clearBackStack = true
                )
            }
        }
    }

    /**
     * Map UserType from authentication to UserRole for navigation permissions
     */
    fun mapUserTypeToRole(userType: UserType): UserRole {
        return when (userType) {
            UserType.EMPLOYER -> UserRole.EMPLOYER
            UserType.EMPLOYEE -> UserRole.EMPLOYEE
        }
    }

    companion object {
        private const val TAG = "AuthNavigationManager"
    }
}