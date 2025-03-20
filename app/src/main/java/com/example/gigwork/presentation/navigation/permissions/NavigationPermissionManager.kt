package com.example.gigwork.presentation.navigation.permissions

import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.UserRole
import com.example.gigwork.presentation.NavigationScope
import com.example.gigwork.presentation.navigation.Screen
import com.example.gigwork.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationPermissionManager @Inject constructor(
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @NavigationScope private val navigationScope: CoroutineScope
) {
    private val publicRoutes = setOf(
        Screen.Welcome.route,
        Screen.Login.route,
        Screen.Signup.route
    )

    private val employerOnlyRoutes = setOf(
        Screen.EmployerDashboard.route,
        Screen.CreateJob.route,
        Screen.EmployerProfile.route
    )

    private val employeeOnlyRoutes = setOf(
        Screen.Jobs.route,
        Screen.EmployeeProfile.route
    )

    suspend fun canNavigateToRoute(route: String, userRole: UserRole): Boolean {
        return withContext(ioDispatcher) {
            val hasPermission = when {
                route in publicRoutes -> true
                route in employerOnlyRoutes -> userRole == UserRole.EMPLOYER
                route in employeeOnlyRoutes -> userRole == UserRole.EMPLOYEE
                else -> true
            }

            if (!hasPermission) {
                logger.w(
                    tag = TAG,
                    message = "Navigation permission denied",
                    additionalData = mapOf(
                        "route" to route,
                        "userRole" to userRole.name
                    )
                )
            }

            hasPermission
        }
    }

    companion object {
        private const val TAG = "NavigationPermissionManager"
    }
}